/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.mergelayers.biaffine

import com.kotlinnlp.simplednn.core.arrays.UpdatableArray
import com.kotlinnlp.simplednn.core.layers.ForwardHelper
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.Shape
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArrayFactory

/**
 * The helper which executes the forward on a biaffine [layer].
 *
 * @property layer the [BiaffineLayerStructure] in which the forward is executed
 */
class BiaffineForwardHelper<InputNDArrayType : NDArray<InputNDArrayType>>(
  override val layer: BiaffineLayerStructure<InputNDArrayType>
) : ForwardHelper<InputNDArrayType>(layer) {

  /**
   * Forward the input to the output combining it with the parameters.
   *
   *   w[ i ] = (wi (dot) x1)' (dot) x2
   *   y = f(w + w1 (dot) x1 + w2 (dot) x2 + b)
   */
  override fun forward() {

    val x1: InputNDArrayType = this.layer.inputArray.values
    val x2: InputNDArrayType = this.layer.inputArray2.values
    val y: DenseNDArray = this.layer.outputArray.values

    val wArrays: Array<UpdatableArray<*>> = this.layer.params.w
    val w: DenseNDArray = DenseNDArrayFactory.emptyArray(Shape(y.length))
    val w1: DenseNDArray = this.layer.params.w1.values as DenseNDArray
    val w2: DenseNDArray = this.layer.params.w2.values as DenseNDArray
    val b: DenseNDArray = this.layer.params.b.values

    wArrays.forEachIndexed { i, wArray ->
      val wi: DenseNDArray = wArray.values as DenseNDArray
      val wxi: DenseNDArray = this.layer.wxArrays[i].values

      wxi.assignDot(wi, x1)
      w[i] = wxi.T.dot(x2)[0] // the result is an array with Shape (1, 1)
    }

    y.assignDot(w1, x1).assignSum(w2.dot(x2)).assignSum(w).assignSum(b)

    this.layer.outputArray.activate()
  }

  /**
   * Forward the input to the output combining it with the parameters, saving the contributions.
   *
   *   w[ i ] = (wi (dot) x1)' (dot) x2
   *   y = f(w + w1 (dot) x1 + w2 (dot) x2 + b
   *
   * @param layerContributions the structure in which to save the contributions during the calculations
   */
  override fun forward(layerContributions: LayerParameters) {
    layerContributions as BiaffineLayerParameters

    TODO("not implemented")
  }
}
