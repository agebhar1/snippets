import unittest

from direct_acyclic_graph import (
    dependency,
    CycleException,
    DirectAcyclicGraph,
    tool,
    NodeNotFoundException,
    EdgeType,
)

G1 = {
    "a": dependency("b"),
    "b": dependency("c"),
    "c": dependency("d"),
    "d": dependency("e"),
    "e": [],
}

G2 = {
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

G3 = {
    "a": [],
    "b": dependency("a"),
    "c": tool("b"),
    "d": dependency("b", "c"),
}


class DirectAcyclicGraphTestCase(unittest.TestCase):
    def test_incomplete(self):
        g = {"a": dependency("b")}

        with self.assertRaises(NodeNotFoundException) as err:
            DirectAcyclicGraph(g)

        self.assertEqual("Node 'b' not found", str(err.exception))

    def test_cycle(self):
        g = {
            "a": dependency("b"),
            "b": dependency("c"),
            "c": dependency("d"),
            "d": dependency("a"),
        }

        with self.assertRaises(CycleException) as err:
            DirectAcyclicGraph(g)

        self.assertEqual("['a', 'b', 'c'] + d -> a", str(err.exception))

    def test_topsort_simple(self):
        dag = DirectAcyclicGraph(g=G1)

        self.assertEqual(["a", "b", "c", "d", "e"], dag.topological_order())

    def test_topsort(self):
        dag = DirectAcyclicGraph(g=G2)

        self.assertEqual(
            ["a", "b", "e", "d", "d2", "d1", "c", "c2", "c2d2", "c1", "c1d1"],
            dag.topological_order(),
        )

    def test_topsort_mixed(self):
        dag = DirectAcyclicGraph(
            g={
                "a": [],
                "b": dependency("a"),
                "c": tool("b"),
                "d": dependency("b", "c"),
            }
        )

        self.assertEqual(["d", "c", "b", "a"], dag.topological_order())

    def test_inverse_simple(self):
        dag = DirectAcyclicGraph(g=G1)

        self.assertEqual(
            {
                "a": [],
                "b": dependency("a"),
                "c": dependency("b"),
                "d": dependency("c"),
                "e": dependency("d"),
            },
            dag.inverse(),
        )

    def test_inverse(self):
        dag = DirectAcyclicGraph(g=G2)

        self.assertEqual(
            {
                "a": [],
                "b": dependency("a"),
                "c": dependency("b"),
                "d": dependency("b"),
                "e": dependency("b"),
                "c1": dependency("c"),
                "c2": dependency("c"),
                "d1": dependency("d"),
                "d2": dependency("d"),
                "c1d1": dependency("c1", "d1"),
                "c2d2": dependency("c2", "d2"),
            },
            dag.inverse(),
        )

    def test_inverse_mixed(self):
        dag = DirectAcyclicGraph(g=G3)

        self.assertEqual(
            {
                "a": dependency("b"),
                "b": tool("c") + dependency("d"),
                "c": dependency("d"),
                "d": [],
            },
            dag.inverse(),
        )

    def test_childs_accept_all(self):
        dag = DirectAcyclicGraph(G3)

        self.assertEqual(["c", "d"], dag.childs("b"))

    def test_childs_accept_specific(self):
        dag = DirectAcyclicGraph(G3)

        self.assertEqual(["d"], dag.childs("b", lambda node: node[1] != EdgeType.TOOL))

    def test_childs_transitive_accept_all(self):
        dag = DirectAcyclicGraph(G3)

        self.assertEqual({"b", "c", "d"}, dag.childs_transitive("a"))

    def test_childs_transitive_accept_specific(self):
        dag = DirectAcyclicGraph(G3)

        self.assertEqual(
            {"b", "d"},
            dag.childs_transitive("a", lambda node: node[1] != EdgeType.TOOL),
        )

    def test_childs_accept_all_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(["c1", "d1"], dag.childs("c1d1"))

    def test_childs_transitive_accept_all_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(
            {"c1", "d1", "c", "d", "b", "a"}, dag.childs_transitive("c1d1")
        )

    def test_none_parent_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual([], dag.parents("e"))

    def test_single_parent_accept_all_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(["c", "d", "e"], dag.parents("b"))

    def test_single_parent_accept_custom_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(
            ["c", "e"], dag.parents("b", accept=lambda adjacent: adjacent[0] != "d")
        )

    def test_multiple_parent_accept_all_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(["c", "d", "e"], dag.parents("b"))

    def test_parents_transitive_accept_all_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual({"c1", "c1d1", "c2", "c2d2"}, dag.parents_transitive("c"))

    def test_parents_transitive_accept_custom_G2(self):
        dag = DirectAcyclicGraph(G2)

        self.assertEqual(
            {"c1", "c1d1"},
            dag.parents_transitive("c", accept=lambda adjacent: adjacent[0] != "c2"),
        )
