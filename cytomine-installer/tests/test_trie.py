


from unittest import TestCase
from cytomine_installer.deployment.util.trie import Trie


class TestTrie(TestCase):
  def testTrieEmpty(self):
    trie = Trie()
    self.assertFalse(trie.has([]))
    self.assertFalse(trie.has(None))
    self.assertFalse(trie.has(["a"]))
    self.assertFalse(trie.has([""]))
    self.assertFalse(trie.has(["*"]))

  def testTrieNoWildcard(self):
    trie = Trie()
    trie.insert(["global", "namespace", "foo"])
    self.assertTrue(trie.has(["global", "namespace", "foo"]))
    self.assertFalse(trie.has([]))
    self.assertFalse(trie.has(["global", "namespace"]))
    self.assertFalse(trie.has(["global"]))
    self.assertFalse(trie.has(["global", "namespace", "bar", "other"]))
    self.assertFalse(trie.has(["global", "namespace", "bar"]))
    self.assertFalse(trie.has([""]))
    self.assertFalse(trie.has(["un", "rela", "ted"]))

  def testTrieWildcardEnd(self):
    trie = Trie()
    trie.insert(["global", "*"])
    self.assertTrue(trie.has(["global", "ns"]))
    self.assertTrue(trie.has(["global", "namespace", "foo"]))
    self.assertFalse(trie.has(["services", "namespace", "foo"]))
    self.assertFalse(trie.has(["global"]))

  def testTrieWildcardInnerself(self):
    trie = Trie()
    trie.insert(["global", "*", "VALUE"])
    self.assertTrue(trie.has(["global", "word", "VALUE"]))
    self.assertTrue(trie.has(["global", "word1", "VALUE"]))
    self.assertFalse(trie.has(["global", "word1", "VALUE2"]))
    self.assertFalse(trie.has(["services", "namespace", "foo"]))
    self.assertFalse(trie.has(["global", "word"]))

  def testTrieInsertAfterWild(self):
    trie = Trie()
    trie.insert(["global", "*"])
    trie.insert(["global", "word", "VALUE"])
    self.assertTrue(trie.has(["global", "ns"]))
    self.assertTrue(trie.has(["global", "namespace", "foo"]))
    self.assertFalse(trie.has(["services", "namespace", "foo"]))
    self.assertFalse(trie.has(["global"]))

  def testTrieInsertBeforeWild(self):
    trie = Trie()
    trie.insert(["global", "word", "VALUE"])
    trie.insert(["global", "*"])
    self.assertTrue(trie.has(["global", "ns"]))
    self.assertTrue(trie.has(["global", "namespace", "foo"]))
    self.assertFalse(trie.has(["services", "namespace", "foo"]))
    self.assertFalse(trie.has(["global"]))

    trie2 = Trie()
    trie2.insert(["global", "word", "VALUE"])
    trie2.insert(["global", "*", "VALUE2"])
    self.assertTrue(trie2.has(["global", "any1", "VALUE2"]))
    self.assertTrue(trie2.has(["global", "any2", "VALUE2"]))
    self.assertTrue(trie2.has(["global", "word", "VALUE"]))
    self.assertTrue(trie2.has(["global", "word", "VALUE2"]))
    self.assertFalse(trie2.has(["global", "word", "VALUE3"]))

