/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers

import com.kotlinnlp.simplednn.core.arrays.AugmentedArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArrayFactory
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.Shape

/**
 * The basic unit of the layer, which extends the [AugmentedArray] with forward and backward methods.
 */
open class LayerUnit<InputNDArrayType : NDArray<InputNDArrayType>>(size: Int) : AugmentedArray<DenseNDArray>(size) {

  init {
    this.assignValues(DenseNDArrayFactory.emptyArray(Shape(size)))
  }

  /**
   * Forward from the given input.
   *
   * g = w (dot) x + b
   *
   * @param gateParams the parameters associated to this unit
   * @param x the input array of the current layer
   */
  fun forward(gateParams: RecurrentParametersUnit, x: InputNDArrayType) {

    val w = gateParams.weights.values as DenseNDArray
    val b = gateParams.biases.values

    this.values.assignDot(w, x).assignSum(b)
  }

  /**
   * Assign errors to the parameters associated to this unit.
   *
   * gb = errors * 1
   * gw = errors (dot) x
   *
   * @param paramsErrors the parameters associated to this unit
   * @param x the input of the unit
   */
  fun assignParamsGradients(paramsErrors: RecurrentParametersUnit, x: InputNDArrayType) {

    val gb: DenseNDArray = paramsErrors.biases.values
    val gw: NDArray<*> = paramsErrors.weights.values

    gb.assignValues(this.errors)
    gw.assignDot(this.errors, x.T)
  }
}
