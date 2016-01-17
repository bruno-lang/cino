Instructiin Set
===============

A main goal of the instruction set is allow compact description of computation 
while still be human readable/writeable. 

The central idea is to just use the visable ASCII character set for instrctions
and leave the remaining range for internal use. 
That means the ranges 0-31 and 128-255 are reserved so that a VM implementation
can rewite the program on load using these instruction codes to replace public
instructions mostly to improve performance of common operations such as loading
of constants.

The second central concept is that operations implicitly refer to their operands.
For example simple arithmetic is done with L and R, the two topmost items on the
stack. Vector operations implictly apply to register 0 and so forth.


Improvements are:

Constant Rewrinting
-------------------
`0` to `9` load the interers 0 to 9, a sequences like `23` would load 23.
Semantically the VM checks if the instruction prior to the current is a constant
as well and in such case does multiplication with 10 and addition so `23` does
not load 2 and 3 but 23.
Here a VM could rewirte constants of 2-3 digits with two bytes, first one being
an internal _load constant 1_ instruction followed by one value byte or a
_load constant 2_ instruction followed by two value bytes for numbers > 255.
Also single digit constants could be rewirten such that `0` (ASCII) becomes
actual 0 and so forth to remove the subtraction required for ASCII to number.


Reduced Stack Movement
----------------------
Typically stack machines push and pop items to or from the stack when executing
an operation. This is a simple model but often does extra work and requires 
extra instructions. A VM can do it differently. 

The two topmost items are called L and R (topmost).
Loads override R (no stack movement).
Arithmetic uses L and R and writes to L (again with no stack movement). 
So to bring to numbers onto the stack one has to use `dup`. 
Yet, programs are usually a seuqnece of operations combinging an intermediate 
result with a new word via some arithmetic operation. With this in mind it is
easy to see that loads to R and stores to L make a lot of sense.
After first operation L holds the intermediate result, R can be overriden with
next part of the whole formular and again combined to next intermediate result
and so forth. 
So while programs often look like classic stack instructions they do a lot less
stack movement by defintion of the instructions semantics. 
