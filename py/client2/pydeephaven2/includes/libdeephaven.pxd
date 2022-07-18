# distutils: language = c++

from libc.stdint cimport int64_t

cdef extern from "deephaven/client/src/types.cpp":
    pass

cdef extern from "include/public/deephaven/client/types.h" namespace "deephaven::client":
    cdef cppclass CDateTime "deephaven::client::DateTime" nogil:
        DateTime() except +
        DateTime(int64_t) except +
        DateTime(int, int, int) except +
        DateTime(int, int, int, int, int, int) except +
        DateTime(int, int, int, int, int, int, long) except +
        int64_t nanos()
        @staticmethod
        DateTime fromNanos(long)
