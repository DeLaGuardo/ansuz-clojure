# ansuz

Ansuz is a library that builds a parser language on top of clojure. It makes
an intensive use of macros, but uses monad combinatorial programming style.
(see http://www.cs.indiana.edu/~jsobel/Parsing/explicit.html)

## Note

Because clojure doesn't support tail call optimization, the parser makes use
of a trampoline, therefore do not return function from parsers. If you have to
wrap them in another data structure, like `[fn]`.

Another solution is to wrap the function in a 'false function' i.e. an object
that implements IFn interface

     (defn fn->ifn [fun]
       (proxy [clojure.lang.IFn] []
         (invoke [s] (fun s))))

## Usage

See src/ansuz/parsers/calc.clj or src/ansuz/parsers/json.clj as an example
NOTE: in this document the input of the parser is always a string, but the
generic type accepted by run is a collecction (i.e. something valid for 
first and rest functions).

## ansuz.core

ansuz.core contains basic parsers elements

### `(fail <reason>)`

make this parser fail. If not differently specified in
run macro, It will raise an error with "some reason" as argument

    (run (fail "some reason") "whatever") 
    -> raise Error "some reason";

### `(ret <val>)` 

returns a value

    (run (ret 10) "whatever") -> 10

### `(any)` 

returns the first token in input

    (run (any) "whatever") -> \w

### `(end)`

matches the end of input

    (run (end) "ciao") -> throws the error "not end"
    (run (end) "") -> true

### `(! <val>)` 

succeed if the first element of the input is val

    (run (! \h) "whatever")
    -> raise Error ("get failed")
    (run (! \w) "whatever") -> \w

### `(? <test>)`

    (run (? #(clojure.string/blank? (str %))) "whatever")
    -> fail
    (run (? #(clojure.string/blank? (str %))) " whatever")
    -> \space

### `(in)` 

this special parser simply returns the input 

    (run (cat (any) (in)) "whatever") -> [\h \a \t \e \v \e \r]

## ansuz.language

this module contains macros for creating, combining, running parsers

### `(run <expr> <input>)`

    (run <expr> <input>)

or

    (run <expr> <input> <fail>)

run the parser defined by expr against the `<input>`.
you can pass an optional function `<fail>` that returns the value
assumed by the whole expression if the parser fails.
the default fail function is `#(throw (Error. %))`

### `(defparser <name> [<arg> ...] <body>)`
define a new parser named <name>. The semantic is similar to defn 
except that <body> ... is passed to the parser language walker 
(@see parser language)
example:

    (defparser zum [a b] (ret (+ a b)))
    (run (zum 10 20) "ciao") -> 30
  
    (defparser if-h-hello [] \h (ret "hello"))
    (run (if-h-hello) "hello") -> "hello"

## parser language

### `(cat <pn> ...)`

concatenate parsers, if <pn> is not a function, it is wrapped with

    (! <pn>) so (cat \h \e \l \l \o) is the same of 
    (cat (! \h) (! \e) (! \l) (! \l) (! \o))

you can retrieve partial parser values using <-

    (cat (<- a (ret "hello")
         (do-something-with a)))

the body of the parser is implicitely passed to cat.

    (run (cat \h \e) "hello") -> \e
    (run (cat (<- h \h) \e (ret h)) "hello") -> \h
   

### `(alt <pn> ...)`   Tries each <pn> parser.

    (run (alt \h \e) "hello") -> \h
    (run (alt \h \e) "environment")  -> \e
   	
### `(alt* <pn> ...)`
This is the *correct* form instead of alt that is more efficient.
you can see the difference using parsers like these

    (cat (alt (cat \a \b) \a) \b)
   
alt* parses it correcly recognizing `"ab"` and `"abb"`, alt instead
recognize only `"abb"`.

### `(let [<key> <val> ...] <p> ...)`
the meaning is the same for clojure, except the whole expression 
is a parser
   
    (run (let [a 10 b 20] \w \h (ret (* a b))) "whatever") -> 200

### `(if <t?> <p> <q>)`
<t?> is a clojure expression, <p> and <q> are parser expressions

    (run (if true \w \y) "whatever") -> \w
    (run (if true \y \w) "whatever") -> fail
    (run (if false \w \y) "whatever") -> fail
    (run (if false \y \w) "whatever") -> \w

### `(cond <t?> <p> ...)`
 <?t> is a clojure expression, <p> a parser expressions,

### ansuz.extra
This library contains a set of parser combinators

### `(maybe <p>)`
Always succeeds, if the parser <p> succeeds consuming the stream
otherwise returning false

    (run (maybe \w) "whatever") -> \w
    (run (maybe \y) "whatever") -> false (this is different from failing)
   
### `(many <p>)`
This is the kleene operator, it try to run <p> as many times it can

    (run (many \a) "aaab") -> [\a \a \a]
    (run (many \a) "bbba") -> []

### `(up <p> <num>)`
It's like many except limits the maximum of repetions to <num>

    (run (up \a 3) "aaaaaaab") -> [\a \a \a]
    (run (up \a 10) "aaabaaa") -> [\a \a \a]
    (run (up \a 10) "baaaaaa") -> []

## ansuz.expressions

  this module contains a single parser that is used to parse expressions 
  with infix/prefix/postfix priority table.

### `(expr <op-table> <term>)`
  `<op-table>` is a clojure map with 3 keys, this is for example the one you 
  can find in calc.
 
    {:prefix [[dif 1]]
      :infix [[sum 1 :left]
              [dif 1 :left]
              [mul 2 :left] 
              [div 2 :left]]
     :postfix [[sqr 3]]}

   The first element is a parser, that parses the operator.  The second is the
   priority, for infix you have a third parameter that is the associativity, 
   `:left` or `:right`.

   <term> is a parser that parse the atomic terms of expressions (or comprised
   between brackets). see calc.clj for a simple and exhaustive example.

## License

Copyright (C) 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
