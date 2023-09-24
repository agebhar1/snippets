import unittest

from direct_acyclic_graph import DirectAcyclicGraph, dependency, tool
from planner import Planner


DAG1 = DirectAcyclicGraph(
    g={
        "a": dependency("b"),
        "b": dependency("c"),
        "c": dependency("d"),
        "d": dependency("e"),
        "e": [],
    }
)

DAG2 = DirectAcyclicGraph(
    {
        "a": dependency("b"),
        "b": dependency("c", "d", "e"),
        "c": dependency("c1", "c2"),
        "d": dependency("d1", "d2"),
        "e": [],
        "c1": dependency("c1d1"),
        "c2": dependency("c2d2"),
        "d1": dependency("c1d1"),
        "d2": dependency("c2d2"),
        "c1d1": [],
        "c2d2": [],
    }
)

DAG3 = DirectAcyclicGraph(
    g={
        "a": [],
        "b": dependency("a"),
        "c": tool("b"),
        "d": dependency("b", "c"),
    }
)


class PlanerTestCase(unittest.TestCase):
    def test_no_action(self):
        planner = Planner(state=DAG1, target=DAG1)

        self.assertEqual([], planner.apply())

    def test_apply_DAG1_modified_multiple(self):
        planner = Planner(state=DAG1, target=DAG1, modified=["b", "d"])

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG1_unhealthy_multiple(self):
        planner = Planner(state=DAG1, target=DAG1, unhealthy=["b", "d"])

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG1_unhealthy_and_modified(self):
        planner = Planner(
            state=DAG1, target=DAG1, modified=["b", "d"], unhealthy=["b", "d"]
        )

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG1_unhealthy_or_modified(self):
        planner = Planner(state=DAG1, target=DAG1, modified=["b"], unhealthy=["d"])

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG1_modified_root(self):
        planner = Planner(state=DAG1, target=DAG1, modified=["e"])

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "-e", "+e", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG1_unhealthy_root(self):
        planner = Planner(state=DAG1, target=DAG1, unhealthy=["e"])

        self.assertEqual(
            ["-a", "-b", "-c", "-d", "-e", "+e", "+d", "+c", "+b", "+a"],
            planner.apply(),
        )

    def test_apply_DAG3_modified_single(self):
        planner = Planner(state=DAG3, target=DAG3, modified=["d"])

        self.assertEqual(["-d", "+d"], planner.apply())

    def test_apply_DAG3_modified_root(self):
        planner = Planner(state=DAG3, target=DAG3, modified=["a"])

        self.assertEqual(["-d", "-b", "-a", "+a", "+b", "+d"], planner.apply())

    def test_apply_add_to_empty(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={}),
            target=DirectAcyclicGraph(g={"a": dependency("b"), "b": []}),
        )

        self.assertEqual(
            ["+b", "+a"],
            planner.apply(),
        )

    def test_apply_add_to_existing(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": dependency("b"), "b": []}),
            target=DirectAcyclicGraph(
                g={"a": dependency("b", "c"), "b": [], "c": dependency("b")}
            ),
        )

        self.assertEqual(["+c"], planner.apply())

    def test_apply_remove_all(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": dependency("b"), "b": []}),
            target=DirectAcyclicGraph(g={}),
        )

        self.assertEqual(
            ["-a", "-b"],
            planner.apply(),
        )

    def test_apply_DAG3_remove_all(self):
        planner = Planner(state=DAG3, target=DirectAcyclicGraph(g={}))

        self.assertEqual(
            ["-d", "-c", "-b", "-a"],
            planner.apply(),
        )

    def test_add_and_delete(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"b": dependency("a"), "a": []}),
            target=DirectAcyclicGraph(g={"c": dependency("a"), "a": []}),
        )

        self.assertEqual(
            ["-b", "+c"],
            planner.apply(),
        )

    def test_modified_but_delete(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"b": dependency("a"), "c": dependency("a"), "a": []}
            ),
            target=DirectAcyclicGraph(g={"b": dependency("a"), "a": []}),
            modified=["c"],
        )

        self.assertEqual(["-c"], planner.apply())

    def test_apply_delete_DAG2_temporarily(self):
        planner = Planner(state=DAG2, target=DAG2, selected=["-c1d1"])

        self.assertEqual(
            ["-a", "-b", "-d", "-d1", "-c", "-c1", "-c1d1"],
            planner.apply(),
        )

    def test_apply_delete_DAG2_temporarily_with_target_changes(self):
        planner = Planner(
            state=DAG2,
            target=DirectAcyclicGraph(
                g={
                    "c": dependency("c1", "c2"),
                    "d": dependency("d1", "d2"),
                    "c1": dependency("c1d1"),
                    "c2": dependency("c2d2"),
                    "d1": dependency("c1d1"),
                    "d2": dependency("c2d2"),
                    "c1d1": [],
                    "c2d2": [],
                }
            ),
            selected=["-c1d1"],
        )

        self.assertEqual(
            ["-a", "-b", "-d", "-d1", "-c", "-c1", "-c1d1"],
            planner.apply(),
        )

    def test_apply_delete_roots_DAG2_temporarily(self):
        planner = Planner(state=DAG2, target=DAG2, selected=["-c1d1", "-c2d2"])

        actual = planner.apply()
        self.assertEqual(
            ["-a", "-b", "-d", "-d2", "-d1", "-c", "-c2", "-c2d2", "-c1", "-c1d1"],
            actual,
        )
        self.assertNotIn("e", actual)

    def test_apply_delete_with_modified_one_TOOL_edge(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": [], "b": dependency("a"), "c": tool("b")}),
            target=DirectAcyclicGraph(
                g={"a": [], "b": dependency("a"), "c": tool("b")}
            ),
            modified=["b"],
            selected=["-c"],
        )
        self.assertEqual(["-c"], planner.apply())

    def test_apply_delete_with_unhealthy_one_TOOL_edge(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": [], "b": dependency("a"), "c": tool("b")}),
            target=DirectAcyclicGraph(
                g={"a": [], "b": dependency("a"), "c": tool("b")}
            ),
            unhealthy=["b"],
            selected=["-c"],
        )
        self.assertEqual(["-b", "+b", "-c"], planner.apply())

    def test_apply_delete_with_one_modified_two_TOOL_edge_in_chain(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            target=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            modified=["b2"],
            selected=["-c"],
        )
        self.assertEqual(["-c"], planner.apply())

    def test_apply_delete_with_one_unhealthy_two_TOOL_edge_in_chain(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            target=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            unhealthy=["b2"],
            selected=["-c"],
        )
        self.assertEqual(["-b2", "+b2", "-c"], planner.apply())

    def test_apply_delete_with_two_modified_two_TOOL_edge_in_chain(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            target=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            modified=["b1", "b2"],
            selected=["-c"],
        )
        self.assertEqual(["-c"], planner.apply())

    def test_apply_delete_with_two_unhealthy_two_TOOL_edge_in_chain(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            target=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            unhealthy=["b1", "b2"],
            selected=["-c"],
        )
        self.assertEqual(["-b2", "-b1", "+b1", "+b2", "-c"], planner.apply())

    def test_apply_delete_with_one_modified_two_TOOL_edge_in_chain_skip(self):
        planner = Planner(
            state=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            target=DirectAcyclicGraph(
                g={"a": [], "b1": dependency("a"), "b2": tool("b1"), "c": tool("b2")}
            ),
            modified=["b1"],
            selected=["-c"],
        )
        self.assertEqual(["-c"], planner.apply())

    def test_no_action_apply_selected(self):
        planner = Planner(state=DAG1, target=DAG1, modified=[], selected=["c"])

        self.assertEqual([], planner.apply())

    def test_apply_DAG1_modified_multiple_single_selected(self):
        planner = Planner(state=DAG1, target=DAG1, modified=["b", "d"], selected=["b"])

        self.assertEqual(["-a", "-b", "+b", "+a"], planner.apply())

    def test_apply_DAG3_selected_target(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": dependency()}), target=DAG3, selected=["c"]
        )

        self.assertEqual(["+b", "+c"], planner.apply())

    def test_apply_DAG3_selected_target_modified(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": dependency()}),
            target=DAG3,
            modified=["a"],
            selected=["c"],
        )

        self.assertEqual(["+b", "+c"], planner.apply())

    def test_apply_DAG3_selected_target_unhealthy(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": dependency()}),
            target=DAG3,
            unhealthy=["a"],
            selected=["c"],
        )

        self.assertEqual(["-a", "+a", "+b", "+c"], planner.apply())

    def test_apply_DAG3_selected_target_modified_one_TOOL_edge(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": [], "b": dependency("a")}),
            target=DirectAcyclicGraph(
                g={"a": [], "b": dependency("a"), "c": tool("b")}
            ),
            modified=["b"],
            selected=["c"],
        )

        self.assertEqual(["+c"], planner.apply())

    def test_apply_DAG3_selected_target_unhealthy_one_TOOL_edge(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": [], "b": dependency("a")}),
            target=DirectAcyclicGraph(
                g={"a": [], "b": dependency("a"), "c": tool("b")}
            ),
            unhealthy=["b"],
            selected=["c"],
        )

        self.assertEqual(["-b", "+b", "+c"], planner.apply())

    def test_apply_DAG3_selected_target_modified_one_TOOL_edge_chain_skip(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": [], "b": dependency("a"), "c": tool("b")}),
            target=DirectAcyclicGraph(
                g={
                    "a": [],
                    "b": dependency("a"),
                    "c": tool("b"),
                    "d": tool("c"),
                    "e": dependency("d"),
                }
            ),
            modified=["b"],
            selected=["e"],
        )

        self.assertEqual(["+d", "+e"], planner.apply())

    def test_apply_selected_TOOL_edge_modified(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": tool("b"), "b": []}),
            target=DirectAcyclicGraph(g={"a": tool("b"), "b": []}),
            modified=["b"],
            selected=["-a"],
        )

        self.assertEqual(["-a"], planner.apply())

    def test_apply_selected_TOOL_edge_unhealthy(self):
        planner = Planner(
            state=DirectAcyclicGraph(g={"a": tool("b"), "b": []}),
            target=DirectAcyclicGraph(g={"a": tool("b"), "b": []}),
            unhealthy=["b"],
            selected=["-a"],
        )

        self.assertEqual(["-b", "+b", "-a"], planner.apply())
