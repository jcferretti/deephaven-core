# distutils: language = c++

from libc.stdint cimport int64_t

cdef extern from "src/types.cc":
    pass

cdef extern from "include/public/deephaven/client/types.h" namespace "deephaven::client":
    cdef cppclass CDateTime "deephaven::client::DateTime" nogil:
        CDateTime() except +
        CDateTime(int64_t) except +
        CDateTime(int, int, int) except +
        CDateTime(int, int, int, int, int, int) except +
        CDateTime(int, int, int, int, int, int, long) except +
        int64_t nanos()
        @staticmethod
        CDateTime fromNanos(long)
