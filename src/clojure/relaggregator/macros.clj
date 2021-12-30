(ns relaggregator.macros)


(defmacro attempt
  "Attempt att, return att's value if successful,
   return else if an exception is thrown"
  [att else]
  `(try ~att (catch Exception _# ~else)))


(defmacro go-inf
  "Shorthand for go while true do"
  [& fs]
  `(go (while true (do ~@fs))))
