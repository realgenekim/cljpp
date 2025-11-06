(ns cljp.assembler-test
  (:require [clojure.test :refer [deftest is testing]]
            [cljp.assembler :as assembler]))

(deftest assemble-simple-list
  (testing "Empty list"
    (let [tokens [[:push "("] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list)] (:forms result)))))

  (testing "List with one atom"
    (let [tokens [[:push "("] [:atom "defn"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list 'defn)] (:forms result)))))

  (testing "List with multiple atoms"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list 'defn 'foo)] (:forms result))))))

(deftest assemble-vectors
  (testing "Empty vector"
    (let [tokens [[:push "["] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [[]] (:forms result)))))

  (testing "Vector with atoms"
    (let [tokens [[:push "["] [:atom "x"] [:atom "y"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [['x 'y]] (:forms result))))))

(deftest assemble-maps
  (testing "Empty map"
    (let [tokens [[:push "{"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [{}] (:forms result)))))

  (testing "Map with key-value pairs"
    (let [tokens [[:push "{"] [:atom ":a"] [:atom "1"] [:atom ":b"] [:atom "2"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [{:a 1 :b 2}] (:forms result)))))

  (testing "Map with symbol keys"
    (let [tokens [[:push "{"] [:atom "foo"] [:atom "42"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [{'foo 42}] (:forms result))))))

(deftest assemble-nested-structures
  (testing "List containing vector"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"]
                  [:push "["] [:atom "x"] [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list 'defn 'foo ['x])] (:forms result)))))

  (testing "List containing map"
    (let [tokens [[:push "("] [:atom "def"] [:atom "config"]
                  [:push "{"] [:atom ":port"] [:atom "3000"] [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list 'def 'config {:port 3000})] (:forms result)))))

  (testing "Vector containing lists"
    (let [tokens [[:push "["]
                  [:push "("] [:atom "inc"] [:atom "x"] [:pop]
                  [:push "("] [:atom "dec"] [:atom "x"] [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [[(list 'inc 'x) (list 'dec 'x)]] (:forms result))))))

(deftest assemble-function-definition
  (testing "Simple function"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"]
                  [:push "["] [:atom "x"] [:pop]
                  [:push "("] [:atom "inc"] [:atom "x"] [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [(list 'defn 'foo ['x] (list 'inc 'x))] (:forms result)))))

  (testing "Function with let binding"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "process"]
                  [:push "["] [:atom "data"] [:pop]
                  [:push "("] [:atom "let"]
                  [:push "["] [:atom "x"] [:atom "1"] [:pop]
                  [:push "("] [:atom "println"] [:atom "x"] [:pop]
                  [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (let [form (first (:forms result))]
        (is (= 'defn (first form)))
        (is (= 'process (second form)))
        (is (= ['data] (nth form 2)))
        (is (= 'let (first (nth form 3))))))))

(deftest assemble-multiple-top-level-forms
  (testing "Multiple defns"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]
                  [:push "("] [:atom "defn"] [:atom "bar"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= 2 (count (:forms result))))
      (is (= [(list 'defn 'foo) (list 'defn 'bar)] (:forms result))))))

(deftest assemble-atom-types
  (testing "Keywords"
    (let [tokens [[:push "["] [:atom ":name"] [:atom ":age"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [[:name :age]] (:forms result)))))

  (testing "Numbers"
    (let [tokens [[:push "["] [:atom "42"] [:atom "3.14"] [:atom "-7"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [[42 3.14 -7]] (:forms result)))))

  (testing "Booleans and nil"
    (let [tokens [[:push "["] [:atom "true"] [:atom "false"] [:atom "nil"] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [[true false nil]] (:forms result)))))

  (testing "Strings"
    (let [tokens [[:push "["] [:atom "\"hello\""] [:atom "\"world\""] [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (is (= [["hello" "world"]] (:forms result))))))

(deftest assemble-error-underflow
  (testing "POP with empty stack"
    (let [tokens [[:pop]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :underflow (get-in result [:error :code])))))

  (testing "Extra POP"
    (let [tokens [[:push "("] [:atom "defn"] [:pop] [:pop]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :underflow (get-in result [:error :code]))))))

(deftest assemble-error-unclosed
  (testing "Unclosed list"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :unclosed (get-in result [:error :code])))
      (is (= 1 (get-in result [:error :depth])))))

  (testing "Multiple unclosed forms"
    (let [tokens [[:push "("] [:atom "defn"] [:push "["] [:atom "x"]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :unclosed (get-in result [:error :code])))
      (is (= 2 (get-in result [:error :depth]))))))

(deftest assemble-error-map-odd-arity
  (testing "Map with odd number of elements"
    (let [tokens [[:push "{"] [:atom ":a"] [:atom "1"] [:atom ":b"] [:pop]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :map-odd-arity (get-in result [:error :code])))))

  (testing "Map with single element"
    (let [tokens [[:push "{"] [:atom ":a"] [:pop]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :map-odd-arity (get-in result [:error :code]))))))

(deftest assemble-error-no-container
  (testing "Atom at top-level"
    (let [tokens [[:atom "defn"]]
          result (assembler/assemble tokens)]
      (is (not (:ok? result)))
      (is (= :no-container (get-in result [:error :code]))))))

(deftest assemble-complex-example
  (testing "Function with deeply nested structure"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "sum3"]
                  [:push "["] [:atom "a"] [:atom "b"] [:atom "c"] [:pop]
                  [:push "("] [:atom "+"]
                  [:push "("] [:atom "+"] [:atom "a"] [:atom "b"] [:pop]
                  [:atom "c"]
                  [:pop]
                  [:pop]]
          result (assembler/assemble tokens)]
      (is (:ok? result))
      (let [form (first (:forms result))]
        (is (= 'defn (first form)))
        (is (= 'sum3 (second form)))
        (is (= ['a 'b 'c] (nth form 2)))
        (is (= '+ (first (nth form 3))))))))

(deftest assemble-to-string-test
  (testing "Simple function to string"
    (let [tokens [[:push "("] [:atom "defn"] [:atom "foo"]
                  [:push "["] [:atom "x"] [:pop]
                  [:push "("] [:atom "inc"] [:atom "x"] [:pop]
                  [:pop]]
          result (assembler/assemble-to-string tokens)]
      (is (:ok? result))
      (is (string? (:source result)))
      (is (re-find #"defn" (:source result)))
      (is (re-find #"foo" (:source result)))))

  (testing "Error returns error map"
    (let [tokens [[:push "("] [:atom "defn"]]
          result (assembler/assemble-to-string tokens)]
      (is (not (:ok? result)))
      (is (= :unclosed (get-in result [:error :code]))))))
