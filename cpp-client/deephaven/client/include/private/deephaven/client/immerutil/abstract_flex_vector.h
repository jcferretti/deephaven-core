#pragma once

#include <memory>
#include <immer/flex_vector.hpp>
#include <immer/flex_vector_transient.hpp>
#include <arrow/array.h>
#include "deephaven/client/column/column_source.h"
#include "deephaven/client/immerutil/immer_column_source.h"
#include "deephaven/client/utility/utility.h"

namespace deephaven::client::immerutil {
namespace internal {
template<typename T>
struct CorrespondingArrowArrayType {};

template<>
struct CorrespondingArrowArrayType<int32_t> {
  typedef arrow::Int32Array type_t;
};

template<>
struct CorrespondingArrowArrayType<int64_t> {
  typedef arrow::Int64Array type_t;
};

template<>
struct CorrespondingArrowArrayType<double> {
  typedef arrow::DoubleArray type_t;
};
}  // namespace internal
template<typename T>
class AbstractFlexVector;

/**
 * This class allows us to manipulate an immer::flex_vector without needing to know what type
 * it's instantiated on.
 */
class AbstractFlexVectorBase {
protected:
  typedef deephaven::client::column::ColumnSource ColumnSource;
public:
  template<typename T>
  static std::unique_ptr<AbstractFlexVectorBase> create(immer::flex_vector<T> vec);

  virtual ~AbstractFlexVectorBase();

  virtual std::unique_ptr<AbstractFlexVectorBase> take(size_t n) = 0;
  virtual void inPlaceDrop(size_t n) = 0;
  virtual void inPlaceAppend(std::unique_ptr<AbstractFlexVectorBase> other) = 0;
  virtual void inPlaceAppendArrow(const arrow::Array &data) = 0;

  virtual std::shared_ptr<ColumnSource> makeColumnSource() const = 0;
};

template<typename T>
class AbstractFlexVector final : public AbstractFlexVectorBase {
public:
  explicit AbstractFlexVector(immer::flex_vector<T> vec) : vec_(std::move(vec)) {}

  std::unique_ptr<AbstractFlexVectorBase> take(size_t n) final {
    return create(vec_.take(n));
  }

  void inPlaceDrop(size_t n) final {
    auto temp = std::move(vec_).drop(n);
    vec_ = std::move(temp);
  }

  void inPlaceAppend(std::unique_ptr<AbstractFlexVectorBase> other) final {
    auto *otherVec = deephaven::client::utility::verboseCast<AbstractFlexVector*>(
        DEEPHAVEN_PRETTY_FUNCTION, other.get());
    auto temp = std::move(vec_) + std::move(otherVec->vec_);
    vec_ = std::move(temp);
  }

  void inPlaceAppendArrow(const arrow::Array &data) final {
    typedef typename internal::CorrespondingArrowArrayType<T>::type_t arrowArrayType_t;
    auto *typedArrow = deephaven::client::utility::verboseCast<const arrowArrayType_t*>(
        __PRETTY_FUNCTION__, &data);
    auto transient = vec_.transient();
    for (auto element : *typedArrow) {
      if (!element.has_value()) {
        throw std::runtime_error("TODO(kosak): we are not dealing with null values yet");
      }
      transient.push_back(*element);
    }
    vec_ = transient.persistent();
  }

  std::shared_ptr<ColumnSource> makeColumnSource() const final {
    return std::make_shared<ImmerColumnSource<T>>(vec_);
  }

private:
  immer::flex_vector<T> vec_;
};

template<typename T>
std::unique_ptr<AbstractFlexVectorBase> AbstractFlexVectorBase::create(immer::flex_vector<T> vec) {
  return std::make_unique<AbstractFlexVector<T>>(std::move(vec));
}
}  // namespace deephaven::client::immerutil