from typing import Union

from direct_acyclic_graph import DirectAcyclicGraph, EdgeType


class Planner:
    _state: DirectAcyclicGraph
    _target: DirectAcyclicGraph
    _modified: list[str]
    _selected: Union[str, list[str]] = "*"

    def __init__(
        self,
        state: DirectAcyclicGraph,
        target: DirectAcyclicGraph,
        modified: list[str] = None,  # TODO naming
        selected: Union[str, list[str]] = "*",  # TODO naming
    ):
        self._state = state
        self._target = target
        self._modified = modified or []
        self._selected = selected

    def apply(self) -> list[str]:
        nodes_state = self._state.nodes()
        nodes_target = self._target.nodes()

        nodes_add = set()
        nodes_delete = set()
        nodes_with_edge_type_tool_delete = set()
        modified_transitive = set()

        if self._selected == "*":
            nodes_add = {node for node in nodes_target - nodes_state}
            nodes_delete = {node for node in nodes_state - nodes_target}

            for node in self._modified:
                modified_transitive = modified_transitive.union(
                    self._state.childs_transitive(
                        node, accept=lambda adjacent: adjacent[1] != EdgeType.TOOL
                    ).union({node})
                )
        else:
            # partial apply mode
            for item in self._selected:
                if item.startswith("-"):
                    # delete case
                    node = str(item[1:])
                    has_parent_with_edge_type_tool = len(self._state.parents(node)) > 0
                    if has_parent_with_edge_type_tool:
                        parents_edge_type_tool_transitive = (
                            self._state.parents_transitive(
                                node,
                                accept=lambda adjacent: adjacent[1] == EdgeType.TOOL
                                and adjacent[0] in self._modified,
                            )
                        )
                        modified_transitive = modified_transitive.union(
                            parents_edge_type_tool_transitive
                        )
                        nodes_with_edge_type_tool_delete = (
                            nodes_with_edge_type_tool_delete.union(
                                self._state.childs_transitive(
                                    node,
                                    accept=lambda adjacent: adjacent[1]
                                    != EdgeType.TOOL,
                                ).union({node})
                            )
                        )
                    else:
                        nodes_delete = nodes_delete.union(
                            # use state (graph) instead of target, because target will be mostly ignored in this mode
                            self._state.childs_transitive(
                                node,
                                accept=lambda adjacent: adjacent[1] != EdgeType.TOOL,
                            ).union({node})
                        )

                else:
                    node = item
                    if node in self._modified:  # and node in node_states <- implicit
                        modified_transitive = modified_transitive.union(
                            self._state.childs_transitive(
                                node,
                                accept=lambda adjacent: adjacent[1] != EdgeType.TOOL,
                            ).union({node})
                        )
                    elif node in nodes_target:
                        node_with_parents_transitive = self._target.parents_transitive(
                            node
                        ).union({node})
                        nodes_add = node_with_parents_transitive.difference(nodes_state)
                        for node_add in nodes_add:
                            modified_transitive = modified_transitive.union(
                                self._target.parents_transitive(
                                    node_add,
                                    accept=lambda adjacent: (
                                        adjacent[0] in self._modified
                                    ),
                                )
                            )

        node_action = {node: None for node in nodes_state.union(nodes_target)}

        for node in nodes_add:
            node_action[node] = "add"

        for node in nodes_delete:
            node_action[node] = "delete"

        for node in modified_transitive:
            node_action[node] = "modified"

        operations = []
        for node in self._state.topological_order():
            if node_action[node] in ["delete", "modified"]:
                operations.append(f"-{node}")

        for node in reversed(self._target.topological_order()):
            if node_action[node] in ["add", "modified"]:
                operations.append(f"+{node}")

        # handle node deletion of nodes w/ edges of type TOOL
        node_action = {node: None for node in nodes_state.union(nodes_target)}
        for node in nodes_with_edge_type_tool_delete:
            node_action[node] = "delete"

        for node in self._state.topological_order():
            if node_action[node] in ["delete"]:
                operations.append(f"-{node}")

        return operations
