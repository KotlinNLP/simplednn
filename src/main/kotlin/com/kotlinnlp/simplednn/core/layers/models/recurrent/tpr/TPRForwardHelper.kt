/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.models.recurrent.tpr

import com.kotlinnlp.simplednn.core.layers.Layer
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.core.layers.helpers.ForwardHelper
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray


/**
 * The helper which executes the forward on a [layer].
 *
 * @property layer the [TPRLayer] in which the forward is executed
 */
class TPRForwardHelper<InputNDArrayType : NDArray<InputNDArrayType>>(
override val layer: TPRLayer<InputNDArrayType>
) : ForwardHelper<InputNDArrayType>(layer) {

  /**
   *
   */
  private fun addRecurrentContribution(prevStateLayer: Layer<*>?) {
    val params = this.layer.params as TPRLayerParameters

    if (prevStateLayer != null) {
      val yPrev: DenseNDArray = prevStateLayer.outputArray.values

      this.layer.aR.values.assignSum(params.wRecR.values.dot(yPrev))
      this.layer.aS.values.assignSum(params.wRecS.values.dot(yPrev))
    }
  }

  /**
   * Reshape binding Matrix
   */
  private fun vectorizeOutput() {

    var i = 0

    for (r in 0 until layer.bindingMatrix.values.rows)
      for (c in 0 until layer.bindingMatrix.values.columns){

        this.layer.outputArray.values[i] = layer.bindingMatrix.values[r, c]
        i++
      }
  }

  /**
   * Forward the input to the output combining it with the parameters.
   */
  override fun forward() {

    val params = this.layer.params as TPRLayerParameters
    val prevStateLayer = this.layer.layerContextWindow.getPrevState()
    val x: InputNDArrayType = this.layer.inputArray.values

    this.layer.aR.forward(w = params.wInR.values, b = params.bR.values, x = x)
    this.layer.aS.forward(w = params.wInS.values, b = params.bS.values, x = x)

    if (prevStateLayer != null){
      this.addRecurrentContribution(prevStateLayer)
    }

    this.layer.aR.activate()
    this.layer.aS.activate()

    this.layer.r.forward(w = params.R.values, b = null, x = this.layer.aR.values)
    this.layer.s.forward(w = params.S.values, b = null, x = this.layer.aS.values)

    this.layer.bindingMatrix.values.assignValues(a = this.layer.s.values.dot(this.layer.r.values.t))

    this.vectorizeOutput()
  }

  /**
   * Forward the input to the output combining it with the parameters, saving the contributions.
   *
   * @param layerContributions the structure in which to save the contributions during the calculations
   */
  override fun forward(layerContributions: LayerParameters<*>) {
      throw NotImplementedError("Forward with contributions not available for the TPR layer.")

  }
}