(ns clansi.core
	"Collection of utilities to colorize terminal output by embedding ansi-code in the strings to print.
 List of available ansi-code directives as keywords are: (keys ANSI-CODES).
 A call like (ansi :blue), will return the ascii-code string to turn blue-printing on.
 A number of helper functions are available to make the mark-up of strings with ansi-code directives easier:
 (clansify \"this is printed in \" :red \"red\" :reset \", while this is \" :bright :green :underline \"bold&green&underlined.\")
 In addition to the basic ansi-codes, one can also maintain combinations/sequence of codes in the *ANSI-STYLES* map. The map keys as keywords can be used as colorizing directives in the clansify function, like:
 (clansify :protected \"this is protected text\" :reset \", while \" :unprotected \"this is an unprotected string.\")
 Lastly, there is also a \"cdoc\" macro which is a colorized substitute for the venerable clojure.repl/doc macro.")

(def ANSI-CODES
  {:reset              "[0m"
   :bright             "[1m"
   :blink-slow         "[5m"
   :underline          "[4m"
   :underline-off      "[24m"
   :inverse            "[7m"
   :inverse-off        "[27m"
   :strikethrough      "[9m"
   :strikethrough-off  "[29m"

   :default "[39m"
   :white   "[37m"
   :black   "[30m"
   :red     "[31m"
   :green   "[32m"
   :blue    "[34m"
   :yellow  "[33m"
   :magenta "[35m"
   :cyan    "[36m"

   :bg-default "[49m"
   :bg-white   "[47m"
   :bg-black   "[40m"
   :bg-red     "[41m"
   :bg-green   "[42m"
   :bg-blue    "[44m"
   :bg-yellow  "[43m"
   :bg-magenta "[45m"
   :bg-cyan    "[46m"
   })

(def ^:dynamic *ANSI-STYLES* 
	{:white-on-black [:white :bg-black]
	 :protected      [:green :bright] 
	 :unprotected    [:red :bright]
	 :line  :blue
	 :title :bright
	 :args  :red
	 :macro :blue
	 :doc   :green})


(def ^:dynamic *use-ansi* "Rebind this to false if you don't want to see ANSI codes in some part of your code." true)

(defn ansi
  "Output an ANSI escape code using a style key.

   (ansi :blue)
   (ansi :underline)

  Note, try (style-test-page) to see all available styles.

  If *use-ansi* is bound to false, outputs an empty string instead of an
  ANSI code. You can use this to temporarily or permanently turn off
  ANSI color in some part of your program, while maintaining only 1
  version of your marked-up text.
  "
  [code]
  (if *use-ansi*
    (str \u001b (get ANSI-CODES code (:reset ANSI-CODES)))
    ""))

(defmacro without-ansi
  "Runs the given code with the use-ansi variable temporarily bound to
  false, to suppress the production of any ANSI color codes specified
  in the code."
  [& code]
  `(binding [*use-ansi* false]
     ~@code))

(defmacro with-ansi
  "Runs the given code with the use-ansi variable temporarily bound to
  true, to enable the production of any ANSI color codes specified in
  the code."
  [& code]
  `(binding [*use-ansi* true]
     ~@code))


(defn clansify-helper
	"Helper function for clansify that transforms a single sequence of keywords and strings without \"[..]\"."
	[& s] 
	(apply str  
		(ansi :reset)
		(apply str 
			(for [c s] 
				(if (keyword? c) 
					(if-let [codes (c *ANSI-STYLES*)]
						(let [codes (if (sequential? codes) codes [codes])]
							(apply str (map ansi codes)))
							(ansi c))
					(if-let [codes (:ansi-codes (meta c))]
						(str (apply str (map ansi codes)) c)
						c))))
		(ansi :reset)))


(defn clansify
	"Takes a sequence of strings and keywords, where the keywords identify ansi-color-code directives.
	The ansi-code directives only affect the strings that follow it. 
	Returns a single string of the concatenations of the individual strings in the list, where each of these strings is prepended with the accumulated ansi-codes.
	Example with ansi-codes:
	(clansify \"this is \" :red \"red\" :reset \", while this is \" :bright :green :underline \"bold&green&underlined.\") 
	Example with style-codes defined in the *ANSI-STYLES* map:
	(clansify :protected \"protected text\" :reset \" and \" :unprotected \"an unprotected string.\")
	Subsequences can be used to change the styling only in the sublist - sublist inherits the style 
	that was in effect and can have its own additional directives, but once you pop back up to 
	the higher-level, the sublist directives are forgotten. 
	Example of subsequence usage:
	(clansify \"this is \" :red \"red, \" [\"still red, \" :blue \"blue, \"] \"and red again.\") 
"
	[& ansified-strings] 
	(apply str (first 	
		(reduce 
			(fn [strings&keywords item]
				(if (keyword? item) ;; ansi-directive
					[(first strings&keywords) (conj (second strings&keywords) item)]
					(if (sequential? item) ;; format-inheriting substring
						[(conj (first strings&keywords) 
						       (apply clansify (concat (second strings&keywords) item)))
					   (second strings&keywords)]
						(if (var? item) ;; format-inheriting substring
							[(conj (first strings&keywords) 
							       (apply clansify (concat (second strings&keywords) 
							                               (:ansi-codes (meta item))
							                               (deref item))))
							 (second strings&keywords)]
							(if (string? item) ;; format string with accumulated ansi-directives
								[(conj (first strings&keywords)
											 (apply clansify-helper (conj (second strings&keywords) item)))
								 (second strings&keywords)]
								[(conj (first strings&keywords)
											 (apply clansify-helper (conj (second strings&keywords) item)))
								 (second strings&keywords)])))))
	;;						  strings&keywords)))) ;; ignore other types
			[[""] []] 
			ansified-strings))))


(defn style-test-page
  "Print the list of supported ANSI styles, each style name shown
  with its own style."
  []
  (println "\nANSI-CODES:\n")
  (doall
    (map #(println (clansify % (name %))) (sort-by name (keys ANSI-CODES))))
  (println "\n*ANSI-STYLES*:\n")
  (doall
    (map #(println (clansify % (name %))) (sort-by name (keys *ANSI-STYLES*))))
  nil)

(defn print-special-doc-color
  "Print stylized special form documentation."
  [meta-map]
  (println (clansify :line "-------------------------"))
  (println (clansify :title (:name meta-map)))
  (when-let [f (:forms meta-map)]
  	(doseq [s f] 
  	  (println (str "  " (clansify :args s)))))
  (println (clansify :macro "Special Form"))
  (println (str "  " (clansify :doc (:doc meta-map))))
  (println (clansify :doc (str "  Please see http://clojure.org/special_forms#" (:name meta-map)))))

(defn print-namespace-doc-color
  "Print stylized documentation for a namespace."
  [nspace]
  (println (clansify :line "-------------------------"))
  (println (clansify :title (str (ns-name nspace))))
  (println (clansify :macro "Namespace"))
  (println (clansify :doc (str " " (:doc (meta nspace))))))

(defn print-doc-color
  "Print stylized function documentation."
  [v]
  (println (clansify :line "-------------------------"))
  (println (clansify :title (str (ns-name (:ns (meta v))) "/" (:name (meta v)))))
  (print "(")
  (doseq [alist (:arglists (meta v))]
   (print "[" (clansify :args (apply str (interpose " " alist))) "]"))
  (println ")")
  (when (:macro (meta v))
    (println (clansify :macro "Macro")))
  (println "  " (clansify :doc (:doc (meta v)))))


(defmacro cdoc
  "Prints colorized documentation for a var, special form or namespace given its name"
  [name]
  (if-let [special-name ('#{& fn catch try finally} name)]
    (#'print-special-doc-color (#'clojure.repl/special-doc special-name))
    (cond
      (#'clojure.repl/special-doc-map name) 
      	`(#'print-special-doc-color (#'clojure.repl/special-doc '~name))
      (find-ns name) `(#'print-namespace-doc-color (find-ns '~name))
      (resolve name) `(#'print-doc-color (var ~name)))))


;;;;;;; not sure if we need the previous style and wrapper function anymore...

(defn style
  "Applies ANSI color and style to a text string.

   (style \"foo\" :red)
   (style \"foo\" :red :underline)
   (style \"foo\" :red :bg-blue :underline)
 "
  [s & codes]
  (str (apply str (map ansi codes)) s (ansi :reset)))

(defn wrap-style
  "Wraps a base string with a stylized wrapper.
  If the wrapper is a string it will be placed on both sides of the base,
  and if it is a seq the first and second items will wrap the base.

  To wrap debug with red brackets => [debug]:

  (wrap-style \"debug\" [\"[\" \"]\"] :red)
  "
  [base wrapper & styles]
  (str (apply style (if (coll? wrapper) (first wrapper) wrapper) styles)
       base
       (apply style (if (coll? wrapper) (second wrapper) wrapper) styles)))

