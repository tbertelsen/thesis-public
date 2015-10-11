TODO:

 Test spark kmers.cache vs not cache

 Try to unpersist a lot of stuff

  Ret all  alignment => Identity
  Lav default object, som all metoder refererer.

  Ret corr => proxy

 - Calc merge calc ExactProxy and CalcID to one
    - Calc profiles again, instead of caching them, and sending


Tests to do:
=======

Scalability
------------------
Can use benchmark command
 x Scale servers for fixed dataset
 x Scale dataset for fixed servers

Configurations
------------------
Can use benchmark command.
 - Compare
   - Timings
   - AvgInformation
 - Between
   - calc-exact-corr
   - proxy-centers
 - And with baselinges.
   - uclust (fast / slow?)
   - cdhit (fast / slow?)




DONE
==========
Remove DNA seq from InputGraph when you have the identity already – work only with node ID.


Corrrelation
---------------
 These will require a separate test set
 1. Plot accuracy of aproxximater correlation for different lambdas
 2. Redo analysis of cutoff / time / etc. (tunetest.sh)
    - Find a way to handle false positives!!!!
 3. Lav precision vs recall plots igen, for approximate og exact cutoffs
