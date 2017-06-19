/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.recurrent.ran

import com.kotlinnlp.simplednn.core.layers.ForwardHelper
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.core.layers.LayerStructure
import com.kotlinnlp.simplednn.core.layers.feedforward.FeedforwardLayerParameters
import com.kotlinnlp.simplednn.core.layers.recurrent.GateParametersUnit
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The helper which executes the forward on a [layer].
 *
 * @property layer the [RANLayerStructure] in which the forward is executed
 */
class RANForwardHelper<InputNDArrayType : NDArray<InputNDArrayType>>(
  override val layer: RANLayerStructure<InputNDArrayType>
) : ForwardHelper<InputNDArrayType>(layer) {

  /**
   * Forward the input to the output through the gates, combining it with the parameters.
   *
   * y = f(inG * c + yPrev * forG)
   */
  override fun forward() {

    val prevStateLayer = this.layer.layerContextWindow.getPrevStateLayer()

    this.setGates(prevStateLayer) // must be called before accessing to the activated values of the gates

    val y: DenseNDArray = this.layer.outputArray.values
    val c: DenseNDArray = this.layer.candidate.values
    val inG: DenseNDArray = this.layer.inputGate.values
    val forG: DenseNDArray = this.layer.forgetGate.values

    // y = inG * c
    y.assignProd(inG, c)

    // y += yPrev * forG
    if (prevStateLayer != null) {
      val yPrev: DenseNDArray = prevStateLayer.outputArray.valuesNotActivated
      y.assignSum(yPrev.prod(forG))
    }

    // f(y)
    this.layer.outputArray.activate()
  }

  /**
   * Forward the input to the output through the gates, combining it with the parameters and saving the contributions
   * of the input array to each gate.
   *
   * @param paramsContributions the [LayerParameters] in which to save the contributions of the parameters
   */
  override fun forward(paramsContributions: LayerParameters) {

    val prevStateLayer = this.layer.layerContextWindow.getPrevStateLayer()

    // must be called before accessing to the activated values of the gates
    this.setGates(prevStateLayer = prevStateLayer, paramsContributions = paramsContributions as RANLayerParameters)

    val y: DenseNDArray = this.layer.outputArray.values
    val c: DenseNDArray = this.layer.candidate.values
    val inG: DenseNDArray = this.layer.inputGate.values
    val forG: DenseNDArray = this.layer.forgetGate.values

    // y = inG * c
    y.assignProd(inG, c)

    // y += yPrev * forG
    if (prevStateLayer != null) {
      val yPrev: DenseNDArray = prevStateLayer.outputArray.valuesNotActivated
      val yRec: DenseNDArray = paramsContributions.candidate.biases.values
      // a tricky way to save the recurrent contribution (b.size == y.size)

      yRec.assignProd(yPrev, forG)
      y.assignSum(yRec)
    }

    // f(y)
    this.layer.outputArray.activate()
  }

  /**
   * Set gates values.
   *
   * inG = sigmoid(wIn (dot) x + bIn + wrIn (dot) yPrev)
   * forG = sigmoid(wForG (dot) x + bForG + wrForG (dot) yPrev)
   * c = wc (dot) x + bc
   *
   * @param prevStateLayer the layer in the previous state
   */
  private fun setGates(prevStateLayer: LayerStructure<*>?) { this.layer.params as RANLayerParameters

    val x: InputNDArrayType = this.layer.inputArray.values
    val c: DenseNDArray = this.layer.candidate.values
    val wc: DenseNDArray = this.layer.params.candidate.weights.values as DenseNDArray
    val bc: DenseNDArray = this.layer.params.candidate.biases.values

    this.layer.inputGate.forward(this.layer.params.inputGate, x)
    this.layer.forgetGate.forward(this.layer.params.forgetGate, x)
    c.assignDot(wc, x).assignSum(bc)

    if (prevStateLayer != null) { // recurrent contribution for input and forget gates
      val yPrev = prevStateLayer.outputArray.valuesNotActivated
      this.layer.inputGate.addRecurrentContribution(this.layer.params.inputGate, yPrev)
      this.layer.forgetGate.addRecurrentContribution(this.layer.params.forgetGate, yPrev)
    }

    this.layer.inputGate.activate()
    this.layer.forgetGate.activate()
  }

  /**
   * Set gates values, saving the contributions of the input in respect of the output.
   *
   * inG = sigmoid(wIn (dot) x + bIn + wrIn (dot) yPrev)
   * forG = sigmoid(wForG (dot) x + bForG + wrForG (dot) yPrev)
   * c = wc (dot) x + bc
   *
   * @param prevStateLayer the layer in the previous state
   * @param paramsContributions the [RANLayerParameters] in which to save the contributions of the input in
   *                          respect of the output
   */
  private fun setGates(prevStateLayer: LayerStructure<*>?, paramsContributions: RANLayerParameters) {
    this.layer.params as RANLayerParameters

    // if there's a recurrent contribution biases are divided equally within the sum
    val splitBiases: Boolean = prevStateLayer != null
    val inGParams: GateParametersUnit = this.layer.params.inputGate
    val forGParams: GateParametersUnit = this.layer.params.forgetGate
    val bInG: DenseNDArray = if (splitBiases) inGParams.biases.values.div(2.0) else inGParams.biases.values
    val bForG: DenseNDArray = if (splitBiases) forGParams.biases.values.div(2.0) else forGParams.biases.values

    this.forwardGates(paramsContributions = paramsContributions, bInG = bInG, bForG = bForG)

    if (prevStateLayer != null) { // recurrent contribution for input and forget gates
      this.addGatesRecurrentContribution(
        paramsContributions = paramsContributions,
        yPrev = prevStateLayer.outputArray.valuesNotActivated,
        bInG = bInG,
        bForG = bForG)
    }

    this.layer.inputGate.activate()
    this.layer.forgetGate.activate()
  }

  /**
   * Forward the input to the gates, saving its contributions in respect of each gate.
   *
   * g += wRec (dot) yPrev
   *
   * @param paramsContributions the [RANLayerParameters] in which to save the contributions of the input in
   *                          respect of each gate
   * @param bInG the biases array of the input gate
   * @param bForG the biases array of the forget gate
   */
  private fun forwardGates(paramsContributions: RANLayerParameters, bInG: DenseNDArray, bForG: DenseNDArray) {
    this.layer.params as RANLayerParameters

    val x: InputNDArrayType = this.layer.inputArray.values
    val candidateParams: FeedforwardLayerParameters = this.layer.params.candidate
    val inGParams: GateParametersUnit = this.layer.params.inputGate
    val forGParams: GateParametersUnit = this.layer.params.forgetGate

    this.forwardArray(
      contributions = paramsContributions.candidate.weights.values,
      x = x,
      y = this.layer.candidate.values,
      w = candidateParams.weights.values as DenseNDArray,
      b = candidateParams.biases.values)

    this.forwardArray(
      contributions = paramsContributions.inputGate.weights.values,
      x = x,
      y = this.layer.inputGate.values,
      w = inGParams.weights.values as DenseNDArray,
      b = bInG)

    this.forwardArray(
      contributions = paramsContributions.forgetGate.weights.values,
      x = x,
      y = this.layer.forgetGate.values,
      w = forGParams.weights.values as DenseNDArray,
      b = bForG)
  }

  /**
   * Add the recurrent contribution to the gate, saving the contributions of the input in respect of the output.
   *
   * g += wRec (dot) yPrev
   *
   * @param yPrev the output array of the layer in the previous state
   * @param paramsContributions the [RANLayerParameters] in which to save the contributions of the input in
   *                          respect of each gate
   * @param bInG the biases array of the input gate
   * @param bForG the biases array of the forget gate
   */
  private fun addGatesRecurrentContribution(yPrev: DenseNDArray,
                                          bInG: DenseNDArray,
                                          bForG: DenseNDArray,
                                          paramsContributions: RANLayerParameters) {

    this.layer.params as RANLayerParameters

    val inGParams: GateParametersUnit = this.layer.params.inputGate
    val forGParams: GateParametersUnit = this.layer.params.forgetGate

    this.addRecurrentContribution(
      yPrev = yPrev,
      yRec = paramsContributions.inputGate.biases.values, // a tricky way to save the recurrent contribution
      y = this.layer.candidate.values,                  // (b.size == y.size)
      wRec = inGParams.recurrentWeights.values,
      b = bInG,
      contributions = inGParams.recurrentWeights.values
    )

    this.addRecurrentContribution(
      yPrev = yPrev,
      yRec = paramsContributions.forgetGate.biases.values, // a tricky way to save the recurrent contribution
      y = this.layer.candidate.values,                   // (b.size == y.size)
      wRec = forGParams.recurrentWeights.values,
      b = bForG,
      contributions = forGParams.recurrentWeights.values
    )
  }
}