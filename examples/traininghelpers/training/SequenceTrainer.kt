/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package traininghelpers.training

import com.kotlinnlp.simplednn.core.functionalities.activations.Softmax
import com.kotlinnlp.simplednn.core.functionalities.losses.SoftmaxCrossEntropyCalculator
import com.kotlinnlp.simplednn.core.functionalities.losses.LossCalculator
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.layers.StackedLayersParameters
import com.kotlinnlp.simplednn.core.neuralprocessor.recurrent.RecurrentNeuralProcessor
import com.kotlinnlp.simplednn.core.optimizer.ParamsOptimizer
import com.kotlinnlp.simplednn.helpers.Trainer
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.utils.Shuffler
import traininghelpers.validation.SequenceEvaluator
import utils.SequenceExample

/**
 * A helper to train a recurrent model with sequence examples.
 *
 * @param model the neural model to train
 * @param updateMethod the update method helper (Learning Rate, ADAM, AdaGrad, ...)
 * @param lossCalculator a loss calculator
 * @param examples the training examples
 * @param epochs the number of training epochs
 * @param batchSize the size of each batch (default 1)
 * @param evaluator the helper for the evaluation (default null)
 * @param shuffler used to shuffle the examples before each epoch (with pseudo random by default)
 * @param verbose whether to print info about the training progress and timing (default = true)
 */
class SequenceTrainer<NDArrayType: NDArray<NDArrayType>>(
  model: StackedLayersParameters,
  updateMethod: UpdateMethod<*>,
  private val lossCalculator: LossCalculator,
  examples: List<SequenceExample<NDArrayType>>,
  epochs: Int,
  batchSize: Int = 1,
  evaluator: SequenceEvaluator<NDArrayType>? = null,
  shuffler: Shuffler = Shuffler(),
  verbose: Boolean = true
) : Trainer<SequenceExample<NDArrayType>>(
  modelFilename = "",
  optimizers = listOf(ParamsOptimizer(updateMethod)),
  examples = examples,
  epochs = epochs,
  batchSize = batchSize,
  evaluator = evaluator,
  shuffler = shuffler,
  verbose = verbose
) {

  /**
   * The neural processor that uses the model.
   */
  private val neuralProcessor = RecurrentNeuralProcessor<NDArrayType>(model = model, propagateToInput = false)

  /**
   * Require softmax cross-entropy loss to be used with the softmax as output activation function and vice versa.
   */
  init {

    val activation = this.neuralProcessor.model.layersConfiguration.last().activationFunction

    require(
      (this.lossCalculator is SoftmaxCrossEntropyCalculator && activation is Softmax) ||
        (this.lossCalculator !is SoftmaxCrossEntropyCalculator && activation !is Softmax)
    ) {
      "Softmax cross-entropy loss must be used with the softmax as output activation function and vice versa"
    }
  }

  /**
   * Learn from an example (forward + backward).
   *
   * @param example the example used to train the network
   */
  override fun learnFromExample(example: SequenceExample<NDArrayType>) {

    this.neuralProcessor.forward(example.sequenceFeatures)

    val outputSequence: List<DenseNDArray> = this.neuralProcessor.getOutputSequence()
    val outputSequenceErrors: List<DenseNDArray> =
      this.lossCalculator.calculateErrors(outputSequence, example.sequenceOutputGold)

    this.neuralProcessor.backward(outputSequenceErrors)
  }

  /**
   * Accumulate the errors of the model resulting after the call of [learnFromExample].
   */
  override fun accumulateErrors() {
    this.optimizers.single().accumulate(
      this.neuralProcessor.getParamsErrors(copy = this.batchSize > 1), copy = this.batchSize > 1)
  }

  /**
   * Dump the model to file.
   */
  override fun dumpModel() {}
}
