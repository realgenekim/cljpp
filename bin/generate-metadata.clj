#!/usr/bin/env bb
; Generated from generate-metadata.cljpp via CLJ-PP transpiler

(ns generate-metadata (:require [clojure.java.shell :as shell] [clojure.string :as str] [babashka.fs :as fs]))
(defn get-git-commit [] (-> (shell/sh "git" "rev-parse" "HEAD") :out str/trim))
(defn get-git-dirty? [] (-> (shell/sh "git" "status" "--porcelain") :out str/blank? not))
(defn get-git-branch [] (-> (shell/sh "git" "rev-parse" "--abbrev-ref" "HEAD") :out str/trim))
(defn get-prompt-hash [prompt-file] (when (fs/exists? prompt-file) (-> (shell/sh "sha256sum" (str prompt-file)) :out (str/split #"\s+") first)))
(defn get-prompt-lines [prompt-file] (when (fs/exists? prompt-file) (count (str/split-lines (slurp prompt-file)))))
(defn get-clojure-version [] (-> (shell/sh "clojure" "-M" "-e" "(clojure-version)") :out str/trim (str/replace #"\"" "")))
(defn get-java-version [] (System/getProperty "java.version"))
(defn get-os-name [] (str (System/getProperty "os.name") " " (System/getProperty "os.version")))
(defn generate-metadata [{:keys [prompt-file iterations program description]}] {:git {:dirty? (get-git-dirty?), :commit (get-git-commit), :branch (get-git-branch)}, :experiment-id (str "exp-" (java.util.UUID/randomUUID)), :prompt {:file prompt-file, :lines (get-prompt-lines prompt-file), :sha256 (get-prompt-hash prompt-file)}, :environment {:java-version (get-java-version), :clojure-version (get-clojure-version), :os (get-os-name)}, :timestamp (str (java.time.Instant/now)), :parameters {:description description, :iterations iterations, :program program}, :results {:load-errors 0, :success-rate 0.0, :iterations [], :success-count 0, :transpile-errors 0}})
(defn -main [& args] (let [opts (apply hash-map args) prompt-file (get opts "--prompt" "CLJPP-PROMPT.md") iterations (Integer/parseInt (get opts "--iterations" "10")) program (Integer/parseInt (get opts "--program" "3")) description (get opts "--description" "factorial/fibonacci") metadata (generate-metadata {:description description, :prompt-file prompt-file, :iterations iterations, :program program})] (println (pr-str metadata))))
(when (= *file* (System/getProperty "babashka.file")) (apply -main *command-line-args*))
