(ns cljp.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [cljp.core :as core]))

(deftest transpile-simple-forms
  (testing "Simple function"
    (let [source "PUSH ( defn foo PUSH [ x POP PUSH ( inc x POP POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (string? (:clj result)))
      (is (= 1 (count (:forms result))))
      (is (= 'defn (first (first (:forms result)))))))

  (testing "Let binding"
    (let [source "PUSH ( let PUSH [ x 1 POP PUSH ( println x POP POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result))))
      (is (= 'let (first (first (:forms result)))))))

  (testing "Map literal"
    (let [source "PUSH ( def config PUSH { :port 3000 :host \"localhost\" POP POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result))))))

  (testing "Multiple top-level forms"
    (let [source "PUSH ( ns demo.core POP PUSH ( defn foo POP PUSH ( defn bar POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 3 (count (:forms result)))))))

(deftest transpile-error-handling
  (testing "Tokenize error"
    (let [source "PUSH [ x ]"  ; Invalid: ] instead of POP
          result (core/transpile source)]
      (is (not (:ok? result)))
      (is (= :tokenize (get-in result [:error :code])))))

  (testing "Unclosed form"
    (let [source "PUSH ( defn foo PUSH [ x POP"
          result (core/transpile source)]
      (is (not (:ok? result)))
      (is (= :unclosed (get-in result [:error :code])))))

  (testing "Map odd arity"
    (let [source "PUSH ( def bad PUSH { :a 1 :b POP POP"
          result (core/transpile source)]
      (is (not (:ok? result)))
      (is (= :map-odd-arity (get-in result [:error :code])))))

  (testing "Underflow"
    (let [source "POP"
          result (core/transpile source)]
      (is (not (:ok? result)))
      (is (= :underflow (get-in result [:error :code]))))))

(deftest transpile-repl-test
  (testing "Successful transpile returns forms"
    (let [source "PUSH ( defn foo PUSH [ x POP PUSH ( inc x POP POP"
          forms (core/transpile-repl source)]
      (is (vector? forms))
      (is (= 1 (count forms)))
      (is (= 'defn (first (first forms))))))

  (testing "Error throws exception"
    (let [source "PUSH ( defn foo"] ; Unclosed
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Unclosed"
                            (core/transpile-repl source))))))

(deftest transpile-file-test
  (let [temp-dir (System/getProperty "java.io.tmpdir")
        input-path (str temp-dir "/test-" (System/currentTimeMillis) ".cljp")
        expected-output-path (clojure.string/replace input-path #"\.cljp$" ".clj")]

    (testing "Transpile file creates .clj file"
      ;; Write test input
      (spit input-path "PUSH ( defn hello PUSH [ name POP PUSH ( println \"Hello,\" name POP POP")

      ;; Transpile
      (let [output-path (core/transpile-file input-path)]
        (is (= expected-output-path output-path))
        (is (.exists (io/file output-path)))

        ;; Read and verify output
        (let [output-content (slurp output-path)]
          (is (re-find #"defn" output-content))
          (is (re-find #"hello" output-content))
          (is (re-find #"println" output-content))))

      ;; Cleanup
      (io/delete-file input-path true)
      (io/delete-file expected-output-path true))

    (testing "Transpile file with error throws exception"
      ;; Write invalid input
      (spit input-path "PUSH ( defn foo") ; Unclosed

      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Unclosed"
                            (core/transpile-file input-path)))

      ;; Cleanup
      (io/delete-file input-path true))

    (testing "Transpile non-existent file throws"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"not found"
                            (core/transpile-file "/nonexistent/file.cljp"))))

    (testing "Transpile file with wrong extension throws"
      (let [wrong-ext-path (str temp-dir "/test.clj")]
        (spit wrong-ext-path "content")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"must have .cljp extension"
                              (core/transpile-file wrong-ext-path)))
        (io/delete-file wrong-ext-path true)))))

(deftest transpile-complex-examples
  (testing "Factorial function"
    (let [source "PUSH ( defn factorial PUSH [ n POP
                    PUSH ( if PUSH ( <= n 1 POP
                      1
                      PUSH ( * n PUSH ( factorial PUSH ( dec n POP POP POP
                    POP
                  POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result))))
      (let [form (first (:forms result))]
        (is (= 'defn (first form)))
        (is (= 'factorial (second form))))))

  (testing "Let with map"
    (let [source "PUSH ( let PUSH [ m PUSH { :a 1 :b 2 :c 3 POP POP
                    PUSH ( println PUSH ( :a m POP POP
                  POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result))))))

  (testing "Nested function calls"
    (let [source "PUSH ( defn sum3 PUSH [ a b c POP
                    PUSH ( + PUSH ( + a b POP c POP
                  POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result))))))

  (testing "Multiple top-level forms with ns"
    (let [source "PUSH ( ns demo.core POP
                  PUSH ( defn foo PUSH [ x POP PUSH ( inc x POP POP
                  PUSH ( defn bar PUSH [ y POP PUSH ( dec y POP POP"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 3 (count (:forms result))))
      (is (= 'ns (first (first (:forms result)))))
      (is (= 'defn (first (second (:forms result)))))
      (is (= 'defn (first (nth (:forms result) 2)))))))

(deftest transpile-data-structures
  (testing "Vector of maps"
    (let [source "PUSH ( def users
                    PUSH [
                      PUSH { :name \"Alice\" :age 30 POP
                      PUSH { :name \"Bob\" :age 25 POP
                    POP
                  POP"
          result (core/transpile source)]
      (is (:ok? result))
      (let [form (first (:forms result))]
        (is (= 'def (first form)))
        (is (= 'users (second form)))
        (is (vector? (nth form 2)))
        (is (= 2 (count (nth form 2)))))))

  (testing "Map with nested vectors"
    (let [source "PUSH ( def config
                    PUSH {
                      :servers PUSH [ \"server1\" \"server2\" POP
                      :ports PUSH [ 8080 8081 POP
                    POP
                  POP"
          result (core/transpile source)]
      (is (:ok? result))
      (let [form (first (:forms result))
            m (nth form 2)]
        (is (map? m))
        (is (vector? (:servers m)))
        (is (vector? (:ports m)))))))

(deftest transpile-with-comments
  (testing "Comments are ignored"
    (let [source "PUSH ( defn foo ; function definition
                    PUSH [ x POP ; parameter
                    PUSH ( inc x POP ; body
                  POP ; end"
          result (core/transpile source)]
      (is (:ok? result))
      (is (= 1 (count (:forms result)))))))

(deftest transpile-preserves-semantics
  (testing "Transpiled code can be evaluated"
    (let [source "PUSH ( defn add PUSH [ a b POP PUSH ( + a b POP POP"
          result (core/transpile source)
          forms (:forms result)]
      (is (:ok? result))
      ;; Evaluate the form
      (eval (first forms))
      ;; Call the function using eval
      (is (= 5 (eval '(add 2 3)))))))
