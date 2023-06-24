class TrieNode(dict):
    WILDCARD = "*"

    def __init__(self, end_of_sequence=False):
        # end of sequence flag must be set on the trie node mapped by the key of the last word of the sequence
        self._end_of_sequence = end_of_sequence

    def add_child(self, key, child=None, clear=False):
        self[key] = TrieNode() if child is None else child

    def get_child(self, key):
        return self.get(key)

    @property
    def is_end_of_sequence(self):
        return self._end_of_sequence

    def set_end_of_sequence(self):
        self._end_of_sequence = True

    @property
    def is_leaf(self):
        return len(self) == 0

    @property
    def is_wildcard(self):
        return len(self) == 1 and list(self.keys())[0] == self.WILDCARD


class Trie:
    def __init__(self):
        self.root = TrieNode()

    def insert(self, elements):
        current = self.root
        for word in elements:
            if (
                current.is_wildcard
                and current.get_child(TrieNode.WILDCARD).is_end_of_sequence
            ):
                return
            if word not in current:
                current.add_child(word)
            current = current.get_child(word)
        current.set_end_of_sequence()

    def has(self, query):
        return self._has_helper(query, self.root)

    def _has_helper(self, query, node: TrieNode):
        if query is None:
            return False
        if len(query) == 0:
            return node.is_end_of_sequence

        word = query[0]
        query_suffix = query[1:]

        for key, child in node.items():
            word_is_wildcard = word == TrieNode.WILDCARD
            key_is_wildcard = key == TrieNode.WILDCARD
            if (word_is_wildcard and len(query_suffix) == 0) or (
                key_is_wildcard and child.is_end_of_sequence
            ):
                return True
            if (
                word_is_wildcard or key_is_wildcard or word == key
            ) and self._has_helper(query_suffix, child):
                return True

        return False
