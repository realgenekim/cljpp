(ns cljp.tokenizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [cljp.tokenizer :as tokenizer]))

(deftest tokenize-push-operations
  (testing "PUSH with open paren"
    (is (= [[:push "("]]
           (tokenizer/tokenize "PUSH-("))))

  (testing "PUSH with open bracket"
    (is (= [[:push "["]]
           (tokenizer/tokenize "PUSH-["))))

  (testing "PUSH with open brace"
    (is (= [[:push "{"]]
           (tokenizer/tokenize "PUSH-{"))))

  (testing "Multiple PUSH operations"
    (is (= [[:push "("] [:push "["] [:push "{"]]
           (tokenizer/tokenize "PUSH-( PUSH-[ PUSH-{")))))

(deftest tokenize-pop-operations
  (testing "Single POP"
    (is (= [[:pop]]
           (tokenizer/tokenize "POP"))))

  (testing "Multiple POPs"
    (is (= [[:pop] [:pop] [:pop]]
           (tokenizer/tokenize "POP POP POP")))))

(deftest tokenize-atoms
  (testing "Simple symbol"
    (is (= [[:atom "defn"]]
           (tokenizer/tokenize "defn"))))

  (testing "Keyword"
    (is (= [[:atom ":name"]]
           (tokenizer/tokenize ":name"))))

  (testing "Number"
    (is (= [[:atom "42"]]
           (tokenizer/tokenize "42"))))

  (testing "Negative number"
    (is (= [[:atom "-7"]]
           (tokenizer/tokenize "-7"))))

  (testing "Float"
    (is (= [[:atom "3.14"]]
           (tokenizer/tokenize "3.14"))))

  (testing "Boolean true"
    (is (= [[:atom "true"]]
           (tokenizer/tokenize "true"))))

  (testing "Boolean false"
    (is (= [[:atom "false"]]
           (tokenizer/tokenize "false"))))

  (testing "Nil"
    (is (= [[:atom "nil"]]
           (tokenizer/tokenize "nil")))))

(deftest tokenize-string-literals
  (testing "Simple string"
    (is (= [[:atom "\"hello\""]]
           (tokenizer/tokenize "\"hello\""))))

  (testing "String with spaces"
    (is (= [[:atom "\"hello world\""]]
           (tokenizer/tokenize "\"hello world\""))))

  (testing "String with escaped quotes"
    (is (= [[:atom "\"say \\\"hi\\\"\""]]
           (tokenizer/tokenize "\"say \\\"hi\\\"\""))))

  (testing "Empty string"
    (is (= [[:atom "\"\""]]
           (tokenizer/tokenize "\"\"")))))

(deftest tokenize-simple-forms
  (testing "Simple list"
    (is (= [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
           (tokenizer/tokenize "PUSH-( defn foo POP"))))

  (testing "List with vector"
    (is (= [[:push "("] [:atom "defn"] [:atom "foo"]
            [:push "["] [:atom "x"] [:pop]
            [:pop]]
           (tokenizer/tokenize "PUSH-( defn foo PUSH-[ x POP POP"))))

  (testing "Nested calls"
    (is (= [[:push "("] [:atom "+"]
            [:push "("] [:atom "+"] [:atom "a"] [:atom "b"] [:pop]
            [:atom "c"] [:pop]]
           (tokenizer/tokenize "PUSH-( + PUSH-( + a b POP c POP")))))

(deftest tokenize-whitespace-handling
  (testing "Multiple spaces"
    (is (= [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
           (tokenizer/tokenize "PUSH-(    defn   foo   POP"))))

  (testing "Tabs and newlines"
    (is (= [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
           (tokenizer/tokenize "PUSH-(\n\tdefn\n\tfoo\nPOP"))))

  (testing "Leading and trailing whitespace"
    (is (= [[:push "("] [:atom "defn"] [:pop]]
           (tokenizer/tokenize "  PUSH-( defn POP  ")))))

(deftest tokenize-comments
  (testing "Line comment"
    (is (= [[:push "("] [:atom "defn"] [:pop]]
           (tokenizer/tokenize "PUSH-( defn POP ; this is a comment"))))

  (testing "Multiple comments"
    (is (= [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
           (tokenizer/tokenize "PUSH-( ; open list\ndefn foo ; function\nPOP ; close"))))

  (testing "Comment at start"
    (is (= [[:push "("] [:atom "defn"] [:pop]]
           (tokenizer/tokenize "; comment\nPUSH-( defn POP")))))

(deftest tokenize-complex-example
  (testing "Function with let and map"
    (let [source "PUSH-( defn process PUSH-[ data POP
                    PUSH-( let PUSH-[ m PUSH-{ :a 1 :b 2 POP POP
                      PUSH-( println m POP
                    POP
                  POP"]
      (is (= [[:push "("] [:atom "defn"] [:atom "process"]
              [:push "["] [:atom "data"] [:pop]
              [:push "("] [:atom "let"]
              [:push "["] [:atom "m"]
              [:push "{"] [:atom ":a"] [:atom "1"] [:atom ":b"] [:atom "2"] [:pop]
              [:pop]
              [:push "("] [:atom "println"] [:atom "m"] [:pop]
              [:pop]
              [:pop]]
             (tokenizer/tokenize source))))))

(deftest tokenize-error-invalid-closers
  (testing "Bracket ] is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer '\]'"
                          (tokenizer/tokenize "PUSH-[ x POP ]"))))

  (testing "Brace } is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer '\}'"
                          (tokenizer/tokenize "PUSH-{ :a 1 POP }")))))

(deftest tokenize-error-missing-push
  (testing "Open paren without PUSH"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must be preceded by PUSH"
                          (tokenizer/tokenize "( defn foo"))))

  (testing "Open bracket without PUSH"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must be preceded by PUSH"
                          (tokenizer/tokenize "[ x ]")))))

(deftest tokenize-error-missing-pop
  (testing "Close paren without POP"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer"
                          (tokenizer/tokenize "PUSH-( defn )"))))

  (testing "Standalone close paren"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer"
                          (tokenizer/tokenize ")")))))

(deftest tokenize-error-unclosed-string
  (testing "Unclosed string literal"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unclosed string"
                          (tokenizer/tokenize "\"hello")))))

(deftest tokens-round-trip
  (testing "Round-trip: tokens -> source -> tokens"
    (let [original-tokens [[:push "("] [:atom "defn"] [:atom "foo"]
                           [:push "["] [:atom "x"] [:pop]
                           [:push "("] [:atom "inc"] [:atom "x"] [:pop]
                           [:pop]]
          source (tokenizer/tokens->source original-tokens)
          reparsed-tokens (tokenizer/tokenize source)]
      (is (= original-tokens reparsed-tokens)))))

(deftest format-token-test
  (testing "Format PUSH token"
    (is (= "PUSH-(" (tokenizer/format-token [:push "("]))))

  (testing "Format POP token"
    (is (= "POP" (tokenizer/format-token [:pop]))))

  (testing "Format ATOM token"
    (is (= "defn" (tokenizer/format-token [:atom "defn"])))))
