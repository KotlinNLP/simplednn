/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.mergelayers.product

import com.kotlinnlp.simplednn.core.layers.BackwardHelper
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The helper which executes the backward on a [ProductLayerStructure].
 *
 * @property layer the layer in which the backward is executed
 */
class ProductBackwardHelper(override val layer: ProductLayerStructure) : BackwardHelper<DenseNDArray> {

  /**
   * Executes the backward calculating the errors of the parameters and eventually of the input through the SGD
   * algorithm, starting from the preset errors of the output array.
   *
   * @param paramsErrors the errors of the parameters which will be filled
   * @param propagateToInput whether to propagate the errors to the input array
   * @param mePropK the k factor of the 'meProp' algorithm to propagate from the k (in percentage) output nodes with
   *                the top errors (ignored if null)
   */
  override fun backward(paramsErrors: LayerParameters<*>, propagateToInput: Boolean, mePropK: Double?) {

    if (propagateToInput) {
      this.assignLayerGradients()
    }
  }

  /**
   * Assign the the layer gradients.
   *
   * gxi = gy * prod(xj) [j != i]
   */
  private fun assignLayerGradients() {

    val gy: DenseNDArray = this.layer.outputArray.errors

    this.layer.inputArrays.forEachIndexed { i, xi ->

      val j0: Int = if (i == 0) 1 else 0
      val prod: DenseNDArray = this.layer.inputArrays[j0].values

      this.layer.inputArrays.forEachIndexed { j, xj ->
        if (j != j0 && j != i) prod.assignProd(xj.values)
      }

      xi.assignErrorsByProd(prod, gy)
    }
  }
}
