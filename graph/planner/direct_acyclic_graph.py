from enum import Enum
from typing import Callable, Union


class EdgeType(Enum):
    DEPENDENCY = 0
    TOOL = 1


AdjacentEdge = tuple[str, EdgeType]


def dependency(*nodes: str) -> list[AdjacentEdge]:
    return [(node, EdgeType.DEPENDENCY) for node in nodes]


def tool(*nodes: str) -> list[AdjacentEdge]:
    return [(node, EdgeType.TOOL) for node in nodes]


class CycleException(Exception):
    pass


class NodeNotFoundException(Exception):
    pass


class DirectAcyclicGraph:
    _g: dict[str, list[AdjacentEdge]]
    _g_inverse: dict[str, list[AdjacentEdge]]
    _topological_order: list[str]

    def __init__(self, g: dict[str, list[AdjacentEdge]]):
        self._g_inverse = {}
        for u in g.keys():
            if self._g_inverse.get(u) is None:
                self._g_inverse[u] = []

            for v in g[u]:
                if v[0] not in g:
                    raise NodeNotFoundException(f"Node '{v[0]}' not found")

                if self._g_inverse.get(v[0]) is None:
                    self._g_inverse[v[0]] = [(u, v[1])]
                else:
                    self._g_inverse[v[0]].append((u, v[1]))

        self._g = g
        self._topsort()

    def _topsort(self):
        adjacent = {
            u: iter(sorted([v for (v, _) in value])) for (u, value) in self._g.items()
        }
        result = []

        while len(adjacent):
            # print(f"edges: {edges}")
            dfs_stack = [list(adjacent.keys())[0]]
            while dfs_stack:
                # print("-" * 25)
                # print(f"dfs_stack: {dfs_stack}")
                # print(f"adjacent : {adjacent}")
                u = dfs_stack.pop()
                # print(f"node: {u}")
                if u not in result:
                    # print(f"node: {u} not visited")
                    try:
                        v = next(adjacent[u])
                        if v in dfs_stack:  # cycle
                            raise CycleException(f"{dfs_stack} + {u} -> {v}")
                        dfs_stack.append(u)  # recursive return
                        if v not in result:
                            dfs_stack.append(v)  # recursive call
                    except StopIteration:
                        # print(f"finished {u}")
                        del adjacent[u]
                        result.append(u)

        result.reverse()
        self._topological_order = result

    def nodes(self) -> set[str]:
        return {item[0] for item in self._g.items()}

    def topological_order(self) -> list[str]:
        return self._topological_order

    def inverse(self) -> dict[str, list[AdjacentEdge]]:
        return self._g_inverse

    def childs(
        self,
        node: str,
        accept: Callable[[tuple[str, EdgeType]], bool] = lambda _: True,
    ) -> list[str]:
        return [adjacent[0] for adjacent in self._g_inverse[node] if accept(adjacent)]

    def childs_transitive(
        self,
        node: str,
        accept: Callable[[tuple[str, EdgeType]], bool] = lambda _: True,
    ) -> set[str]:
        result = set()
        childs = self.childs(node, accept)
        while childs:
            child = childs.pop()
            childs.extend(self.childs(child, accept))
            result.add(child)

        return result

    def parents(
        self, node: str, accept: Callable[[tuple[str, EdgeType]], bool] = lambda _: True
    ) -> list[str]:
        return [adjacent[0] for adjacent in self._g.get(node) if accept(adjacent)]

    def parents_transitive(
        self,
        node: str,
        accept: Callable[[tuple[str, EdgeType]], bool] = lambda _: True,
    ):
        result = set()
        parents = self.parents(node, accept)
        while parents:
            parent = parents.pop()
            parents.extend(self.parents(parent, accept))
            result.add(parent)

        return result
