CLANSI
------

Collection of utilities to colorize terminal output by embedding ansi-code in the strings to print.
List of available ansi-code directives as keywords are: `(keys ANSI-CODES)`.
 
A call like `(ansi :blue)`, will return the ascii-code string to turn blue-printing on.

A number of helper functions are available to make the mark-up of strings with ansi-code directives easier:

    (clansify \"this is printed in \" :red \"red\" :reset \", while this is \" :bright :green :underline \"bold&green&underlined.\")

In addition to the basic ansi-codes, one can also maintain combinations/sequence of codes in the *ANSI-STYLES* map. The map keys as keywords can be used as colorizing directives in the clansify function, like:

     (clansify :protected \"this is protected text\" :reset \", while \" :unprotected \"this is an unprotected string.\")

Lastly, there is also a \"cdoc\" macro, which is a colorized substitute for the venerable clojure.repl/doc macro.

You can turn the production of ANSI codes on or off by rebinding the
`clansi.core/*use-ansi*` variable at runtime. This allows you to
maintain only one version of your code with the strings marked up for
color, and then turn ANSI on or off as desired, according to the
properties of each output device, user preference, execution context,
etc.

`(without-ansi)` and `(with-ansi)` convenience macros are provided for
this purpose:

    (defn print-colorized [] 
     (println (clansify :red "foo bar")))

    (print-colorized) ;; prints "foo bar" in red
    (without-ansi (print-colorized)) ;; prints plain "foo bar", without any ANSI color codes
    (without-ansi (with-ansi (print-colorized)) (print-colorized)) ;; prints a red "foo bar", then a plaintext "foo bar"


Acknowledgments
---------------

This is a forked and heavily modified version of https://github.com/ams-clj/clansi.

Not sure if this concoction is an improvement... time will tell... if it still seems like an "improvement" in a few weeks, then a pull-request may be appropriate - we'll see.

Thanks to the Mokum Clojurians for the original ideas and code!

Frank Siebenlist.

