#
# Copyright (c) 2016-2022 Deephaven Data Labs and Patent Pending
#
import random
import time
import unittest
from types import SimpleNamespace
from typing import List, Any

import numpy as np

import deephaven

from deephaven import DHError, read_csv, empty_table, SortDirection, AsOfMatchRule, time_table, ugp, dtypes
from deephaven.agg import sum_, weighted_avg, avg, pct, group, count_, first, last, max_, median, min_, std, abs_sum, \
    var, formula, partition
from deephaven.execution_context import make_user_exec_ctx
from deephaven.filters import Filter, and_
from deephaven.html import to_html
from deephaven.pandas import to_pandas
from deephaven.table import Table, dh_vectorize
from tests.testbase import BaseTestCase


class VectorizationTestCase(BaseTestCase):
    def setUp(self):
        super().setUp()
        deephaven.table._test_vectorization = True
        deephaven.table._vectorized_count = 0

    def tearDown(self) -> None:
        deephaven.table._test_vectorization = False
        deephaven.table._vectorized_count = 0
        super().tearDown()

    def test_vectorization_exceptions(self):
        t = empty_table(1)

        @dh_vectorize
        def vectorized_func(p1, p2):
            return p1 + p2

        def auto_func(p1, p2):
            return p1 + p2

        @dh_vectorize
        def no_param_func():
            return random.randint(0, 100)

        with self.subTest("parameter number mismatch"):
            with self.assertRaises(DHError) as cm:
                t1 = t.update("X = vectorized_func(i)")
            self.assertRegex(str(cm.exception), r".*count.*mismatch", )

            with self.assertRaises(DHError) as cm:
                t1 = t.update("X = auto_func(i)")
            self.assertRegex(str(cm.exception), r".*count.*mismatch", )

        with self.subTest("can't cast return value"):
            with self.assertRaises(DHError) as cm:
                t1 = t.update("X = (float)vectorized_func(i, ii)")
            self.assertIn("can't be cast", str(cm.exception))

        with self.subTest("not be part of another expression"):
            with self.assertRaises(DHError) as cm:
                t1 = t.update("X = k + vectorized_func(i, ii)")
            self.assertIn("in another expression", str(cm.exception))

    def test_column_used_twice(self):
        def py_plus(p1, p2) -> int:
            return p1 + p2

        t = empty_table(1).update("X = py_plus(ii, ii)")

        self.assertEqual(deephaven.table._vectorized_count, 1)

    def test_vectorized_no_arg(self):
        def py_random() -> int:
            return random.randint(0, 100)

        t = empty_table(1).update("X = py_random()")

        self.assertEqual(deephaven.table._vectorized_count, 1)

    def test_vectorized_const_arg(self):
        def py_const(seed) -> int:
            random.seed(seed)
            return random.randint(0, 100)

        expected_count = 0
        t = empty_table(10).update("X = py_const(3)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        seed = 10
        t = empty_table(10).update("X = py_const(seed)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const(30*1024*1024*1024)")
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const(30000000000L)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const(100.01)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const(100.01f)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        with self.assertRaises(DHError) as cm:
            t = empty_table(1).update("X = py_const(NULL_INT)")
        self.assertIn("NULL_INT", str(cm.exception))

        def py_const_str(s) -> str:
            return str(random.randint(1, 1000000000)) + "hello " + str(s) + "!"

        t = empty_table(10).update("X = py_const_str(`Deephaven`)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const_str(null)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = empty_table(10).update("X = py_const_str(true)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

        t = t.update("Y = py_const_str(X)")
        expected_count += 1
        self.assertEqual(deephaven.table._vectorized_count, expected_count)

    def test_multiple_formulas(self):
        def pyfunc(p1, p2, p3) -> int:
            return p1 + p2 + p3

        t = empty_table(1).update("X = i").update(["Y = pyfunc(X, i, 33)", "Z = pyfunc(X, ii, 66)"])
        self.assertEqual(deephaven.table._vectorized_count, 2)
        self.assertIn("33", t.to_string(cols=["Y"]))
        self.assertIn("66", t.to_string(cols=["Z"]))

    def test_multiple_formulas_vectorized(self):
        @dh_vectorize
        def pyfunc(p1, p2, p3) -> int:
            return p1 + p2 + p3

        t = empty_table(1).update("X = i").update(["Y = pyfunc(X, i, 33)", "Z = pyfunc(X, ii, 66)"])
        self.assertEqual(deephaven.table._vectorized_count, 3)
        self.assertIn("33", t.to_string(cols=["Y"]))
        self.assertIn("66", t.to_string(cols=["Z"]))

    def test_filters(self):
        def pyfunc_int(p1, p2, p3) -> int:
            return p1 * p2 * p3

        def pyfunc_bool(p1, p2, p3) -> bool:
            return p1 * p2 * p3

        with self.assertRaises(DHError) as cm:
            t = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where("pyfunc_int(I, 3, J)")
        self.assertEqual(deephaven.table._vectorized_count, 0)
        self.assertIn("boolean required", str(cm.exception))

        t = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where("pyfunc_bool(I, 3, J)")
        self.assertEqual(deephaven.table._vectorized_count, 1)
        self.assertGreater(t.size, 1)

    def test_multiple_filters(self):
        def pyfunc_bool(p1, p2, p3) -> bool:
            return p1 * p2 * p3

        conditions = ["pyfunc_bool(I, 3, J)", "pyfunc_bool(i, 10, ii)"]
        filters = Filter.from_(conditions)
        t = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where(filters)
        self.assertEqual(2, deephaven.table._vectorized_count)

        filter_and = and_(filters)
        t1 = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where(filter_and)
        self.assertEqual(4, deephaven.table._vectorized_count)
        self.assertEqual(t1.size, t.size)
        self.assertEqual(9, t.size)

    def test_multiple_filters_vectorized(self):
        @dh_vectorize
        def pyfunc_bool(p1, p2, p3) -> bool:
            return p1 * p2 * p3

        conditions = ["pyfunc_bool(I, 3, J)", "pyfunc_bool(i, 10, ii)"]
        filters = Filter.from_(conditions)
        t = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where(filters)
        self.assertEqual(3, deephaven.table._vectorized_count)

        filter_and = and_(filters)
        t1 = empty_table(10).view(formulas=["I=ii", "J=(ii * 2)"]).where(filter_and)
        self.assertEqual(5, deephaven.table._vectorized_count)
        self.assertEqual(t1.size, t.size)
        self.assertEqual(9, t.size)

    def test_return_types(self):
        def pyfunc_bool() -> bool:
            return True

        def pyfunc_byte() -> np.int8:
            return 100

        def pyfunc_short() -> np.int16:
            return 100

        def pyfunc_int() -> np.int32:
            return 100

        def pyfunc_long() -> np.int64:
            return 100

        def pyfunc_float() -> np.float32:
            return 100

        def pyfunc_double() -> np.float64:
            return 1000

        def pyfunc_str() -> str:
            return "abc"

        def pyfunc_obj() -> object:
            return [1, 2]

        t = empty_table(1).update("X = pyfunc_bool()")
        self.assertEqual(t.columns[0].data_type, dtypes.bool_)
        t = empty_table(1).update("X = pyfunc_byte()")
        self.assertEqual(t.columns[0].data_type, dtypes.byte)
        t = empty_table(1).update("X = pyfunc_short()")
        self.assertEqual(t.columns[0].data_type, dtypes.short)
        t = empty_table(1).update("X = pyfunc_int()")
        self.assertEqual(t.columns[0].data_type, dtypes.int32)
        t = empty_table(1).update("X = pyfunc_long()")
        self.assertEqual(t.columns[0].data_type, dtypes.long)
        t = empty_table(1).update("X = pyfunc_float()")
        self.assertEqual(t.columns[0].data_type, dtypes.float32)
        t = empty_table(1).update("X = pyfunc_double()")
        self.assertEqual(t.columns[0].data_type, dtypes.double)
        t = empty_table(1).update("X = pyfunc_str()")
        self.assertEqual(t.columns[0].data_type, dtypes.string)
        t = empty_table(1).update("X = pyfunc_obj()")
        self.assertEqual(t.columns[0].data_type, dtypes.PyObject)


if __name__ == "__main__":
    unittest.main()