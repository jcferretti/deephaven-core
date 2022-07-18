# cython: profile=False
# distutils: language = c++
# cython: language_level = 3

from pydeephaven2.libdeephaven cimport CDateTime

cdef class DhDateTime:
    cdef CDateTime c_datetime

    def __cinit__(self):
        self.c_datetime = CDateTime()

    def __cinit__(self, int64_t nanos):
        self.c_datetime = CDateTime(nanos)

    def __cinit__(self, int year, int month, int day):
        self.c_datetime = CDateTime(year, month, day)

    def __cinit__(self, int year, int month, int day, int hour, int minute, int second):
        self.c_datetime = CDateTime(year, month, day, hour, minute, second)

    def __cinit__(self, int year, int month, int day, int hour, int minute, int second, long nanos):
        self.c_datetime = CDateTime(year, month, day, hour, minute, second, nanos)

    def nanos():
        return self.c_datetime.nanos()

    def __cinit__(self, CDateTime o):
        self.c_datetime = o

    @staticmethod
    def from_nanos(long nanos):
        return DhDateTime(CDateTime.fromNanos(nanos))

    def __dealloc__(self):
        del self.c_datetime

     