from dataclasses import dataclass, field
from typing import Union

from direct_acyclic_graph import DirectAcyclicGraph, EdgeType


@dataclass
class NodeState:
    present: set[str] = field(default_factory=set)
    active: set[str] = field(default_factory=set)
    action: None | str = None
    transitive_action: set[str] = field(default_factory=set)
    state: set[str] = field(default_factory=set)
    transitive_state: set[str] = field(default_factory=set)
    dependent_edge_types: set[EdgeType] = field(default_factory=set)


class Planner:
    _state: DirectAcyclicGraph
    _target: DirectAcyclicGraph
    _modified: list[str]
    _unhealthy: list[str]
    _selected: Union[str, list[str]] = "*"

    def __init__(
        self,
        state: DirectAcyclicGraph,
        target: DirectAcyclicGraph,
        modified: list[str] = None,  # TODO naming
        unhealthy: list[str] = None,  # TODO naming
        selected: Union[str, list[str]] = "*",  # TODO naming
    ):
        self._state = state
        self._target = target
        self._modified = modified or []
        self._unhealthy = unhealthy or []
        self._selected = selected

    def apply(self) -> tuple[dict[str, NodeState], list[str]]:
        nodes_state = self._state.nodes()
        nodes_target = self._target.nodes()

        nodes_add = (nodes_target - nodes_state).intersection(
            (nodes_target - nodes_state)
            if self._selected == "*"
            else {node for node in self._selected if not node.startswith("-")}
        )
        nodes_delete = (
            (nodes_state - nodes_target)
            if self._selected == "*"
            else {node[1:] for node in self._selected if node.startswith("-")}
        )
        nodes_selected = (
            nodes_state.union(nodes_target)
            if self._selected == "*"
            else {
                node if not node.startswith("-") else node[1:]
                for node in self._selected
            }
        )

        nodes = {
            node: NodeState(
                present=({"state"} if node in nodes_state else set()).union(
                    {"target"} if node in nodes_target else set()
                ),
                active={"user"} if node in nodes_selected else set(),
                action=(
                    "add"
                    if node in nodes_add
                    else "delete"
                    if node in nodes_delete
                    else None
                ),
                state=({"modified"} if node in self._modified else set()).union(
                    {"unhealthy"} if node in self._unhealthy else set()
                ),
                dependent_edge_types=(
                    self._state.dependent_edge_types(node)
                    if self._state.has_node(node)
                    else set()
                ).union(
                    self._target.dependent_edge_types(node)
                    if self._target.has_node(node)
                    else set()
                ),
            )
            for node in (nodes_state.union(nodes_target))
        }

        # add required nodes by parent dependency
        for node in nodes_add:
            parent_transitive = self._target.parents_transitive(node)
            for node_add_transitive in parent_transitive:
                if node_add_transitive not in nodes_state:
                    nodes[node_add_transitive].transitive_action = nodes[
                        node_add_transitive
                    ].transitive_action.union({"add"})

        # delete child nodes
        for node in nodes_delete:
            childs_transitive = self._state.childs_transitive(
                node,
                accept=lambda adjacent: adjacent[1] != EdgeType.TOOL,
            )
            for node_transitive_delete in childs_transitive:
                # if nodes[node_transitive_delete].primary_action != "delete":
                nodes[node_transitive_delete].transitive_action = nodes[
                    node_transitive_delete
                ].transitive_action.union({"delete"})

        # propagate 'modified'/'unhealthy' state
        unhealthy_adjacent_tool_edge_nodes = set()
        for key, value in nodes.items():
            node = key
            state = value.state
            # TODO test if reachable/required from user selected nodes
            if "unhealthy" in state:
                # propagate 'unhealthy' state to children != EdgeType.TOOL
                childs_transitive = self._state.childs_transitive(
                    node,
                    accept=lambda adjacent: adjacent[1] != EdgeType.TOOL,
                )
                for node_transitive_state in childs_transitive:
                    nodes[node_transitive_state].transitive_state = nodes[
                        node_transitive_state
                    ].transitive_state.union({"unhealthy"})

                # collect nodes ('unhealthy')' w/ EdgeType.TOOL in reverse graph ~> incoming edge(s)
                if EdgeType.TOOL in value.dependent_edge_types:
                    unhealthy_adjacent_tool_edge_nodes.add(node)

            if "modified" in state and value.active:
                # propagate 'modified' state to children != EdgeType.TOOL
                childs_transitive = self._state.childs_transitive(
                    node,
                    accept=lambda adjacent: adjacent[1] != EdgeType.TOOL,
                )
                for node_transitive_state in childs_transitive:
                    nodes[node_transitive_state].transitive_state = nodes[
                        node_transitive_state
                    ].transitive_state.union({"modified"})

        operations = []
        # recover unhealthy nodes (required by dependent 'active' nodes)
        if len(unhealthy_adjacent_tool_edge_nodes) > 0:
            for node in reversed(self._state.topological_order()):
                if node in unhealthy_adjacent_tool_edge_nodes:
                    operations.append(f"-{node}")
            for node in self._state.topological_order():
                if node in unhealthy_adjacent_tool_edge_nodes:
                    operations.append(f"+{node}")

        # TODO re-run w/ updated unhealthy list

        nodes_re_add_modified_or_unhealthy = (
            []
        )  # optimization, not evaluate modified/unhealthy again
        # 'delete' run
        # TODO postpone nodes wich are required as tool
        for node in self._state.topological_order():
            value = nodes[node]

            modified = (
                "modified" in value.state
                and value.active
                or "modified" in value.transitive_state
            )
            unhealthy = (
                "unhealthy" in value.state and value.active
            ) or "unhealthy" in value.transitive_state
            delete = "delete" == value.action or "delete" in value.transitive_action

            if not delete and (modified or unhealthy):
                operations.append(f"-{node}")
                nodes_re_add_modified_or_unhealthy.append(node)

            if delete:
                operations.append(f"-{node}")

        # 'add' run
        for node in reversed(self._target.topological_order()):
            value = nodes[node]

            add = "add" == value.action or "add" in value.transitive_action
            if add or node in nodes_re_add_modified_or_unhealthy:
                operations.append(f"+{node}")

        # TODO delete postponed nodes required as tool but selected for 'delete'

        return nodes, operations
