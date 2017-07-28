/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.birnn

import com.kotlinnlp.simplednn.core.functionalities.activations.ActivationFunction
import com.kotlinnlp.simplednn.core.layers.LayerConfiguration
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.neuralnetwork.NeuralNetwork

/**
 * Bidirectional Recursive Neural Network (BiRNN).
 *
 * The class contains the sub-networks which constitute a BiRNN.
 *
 *   Reference:
 *   Mike Schuster and Kuldip K. Paliwal. - Bidirectional recurrent neural networks
 *
 * @property inputType the type of the input layer (Dense, Sparse, SparseBinary)
 * @property inputSize the size of the input layer of each RNN
 * @property hiddenSize the size of the output layer of each RNN
 * @property hiddenActivation the activation function of the output layer
 * @property recurrentConnectionType type of recurrent neural network (e.g. LSTM, GRU, CFN, SimpleRNN)
 * @property outputSize the size of the [BiRNN] output layer results from the concatenation
 *                      of the hidden layers of each RNN
 */
class BiRNN(
  val inputType: LayerType.Input,
  val inputSize: Int,
  val hiddenSize: Int,
  val hiddenActivation: ActivationFunction?,
  val recurrentConnectionType: LayerType.Connection) {

  /**
   * The size of the output layer resulting from the concatenation of the hidden layers of the [leftToRightNetwork] and
   * [rightToLeftNetwork].
   */
  val outputSize: Int = this.hiddenSize * 2

  /**
   * The Recurrent Neural Network to process the sequence left-to-right.
   */
  val leftToRightNetwork = NeuralNetwork(
    LayerConfiguration(
      size = this.inputSize,
      inputType = this.inputType),
    LayerConfiguration(
      size = this.hiddenSize,
      activationFunction = this.hiddenActivation,
      connectionType = this.recurrentConnectionType))

  /**
   * The Recurrent Neural Network to process the sequence right-to-left.
   */
  val rightToLeftNetwork = NeuralNetwork(
    LayerConfiguration(
      size = this.inputSize,
      inputType = this.inputType),
    LayerConfiguration(
      size = this.hiddenSize,
      activationFunction = this.hiddenActivation,
      connectionType = this.recurrentConnectionType))

  /**
   * Check connection to the output layer.
   */
  init {
    require(this.recurrentConnectionType.property == LayerType.Property.Recurrent) {
      "required recurrentConnectionType with Recurrent property"
    }
  }

  /**
   * Initialize the weight of the sub-networks [leftToRightNetwork] and [rightToLeftNetwork] using the default
   * random generator.
   *
   * @return this BiRNN
   */
  fun initialize(): BiRNN {
    this.leftToRightNetwork.initialize()
    this.rightToLeftNetwork.initialize()

    return this
  }
}
