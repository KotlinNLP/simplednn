/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.feedforward

import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.core.layers.RelevanceHelper
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The helper which calculates the relevance of the input of a [layer] respect of its output.
 *
 * @property layer the [FeedforwardLayerStructure] in which to calculate the input relevance
 */
class FeedforwardRelevanceHelper<InputNDArrayType : NDArray<InputNDArrayType>>(
  layer: FeedforwardLayerStructure<InputNDArrayType>
) : RelevanceHelper<InputNDArrayType>(layer) {

  /**
   * Calculate the relevance of the input respect of the output.
   *
   * @param paramsContributions the contributions of the parameters during the last forward
   *
   * @return the relevance of the input respect of the output
   */
  override fun getInputRelevance(paramsContributions: LayerParameters): NDArray<*> {
    paramsContributions as FeedforwardLayerParameters

    return this.calculateRelevanceOfArray(
      x = this.layer.inputArray.values,
      y = this.layer.outputArray.valuesNotActivated,
      yRelevance = this.layer.outputArray.relevance as DenseNDArray,
      contributions = paramsContributions.weights.values
    )
  }
}