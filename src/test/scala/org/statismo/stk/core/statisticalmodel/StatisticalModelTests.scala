package org.statismo.stk.core.statisticalmodel

import scala.language.implicitConversions
import org.statismo.stk.core.io.{ StatismoIO, MeshIO }
import org.statismo.stk.core.kernels._
import org.statismo.stk.core.geometry._
import breeze.linalg.{ DenseVector, DenseMatrix }
import java.io.File
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import org.statismo.stk.core.numerics.{UniformSampler}
import org.statismo.stk.core.registration.RigidTransformationSpace
import org.statismo.stk.core.registration.RigidTransformation
import org.statismo.stk.core.mesh.TriangleMesh
import breeze.stats.distributions.RandBasis
import org.apache.commons.math3.random.MersenneTwister

class StatisticalModelTests extends FunSpec with ShouldMatchers {

  implicit def doubleToFloat(d: Double) = d.toFloat

  org.statismo.stk.core.initialize()

  describe("A statistical model") {


    def compareModels(oldModel: StatisticalMeshModel, newModel: StatisticalMeshModel) {

      for (i <- 0 until 10) {
        val coeffsData = (0 until oldModel.rank).map {_ =>
          breeze.stats.distributions.Gaussian(0,1).draw().toFloat
        }
        val coeffs = DenseVector(coeffsData.toArray)
        val inst = oldModel.instance(coeffs)
        val instNew = newModel.instance(coeffs)
        inst.points.zip(instNew.points)
        .foreach{case (pt1, pt2) =>
          (pt1.toVector - pt2.toVector).norm should be (0.0 plusOrMinus(0.1))}
      }
    }

    it("can be transformed forth and back and yield the same deformations") {
      val path = getClass.getResource("/facemodel.h5").getPath
      val model = StatismoIO.readStatismoMeshModel(new File(path)).get

      val parameterVector = DenseVector[Float](1.5, 1.0, 3.5, Math.PI, -Math.PI / 2.0, -Math.PI)
      val rigidTransform = RigidTransformationSpace[_3D]().transformForParameters(parameterVector)
      val inverseTransform = rigidTransform.inverse.asInstanceOf[RigidTransformation[_3D]]

      val transformedModel = model.transform(rigidTransform)
      val newModel = transformedModel.transform(inverseTransform)
      compareModels(model, newModel)
    }


    it("can change the mean shape and still yield the same shape space") {
      org.statismo.stk.core.initialize()
      val path = getClass().getResource("/facemodel.h5").getPath
      val model = StatismoIO.readStatismoMeshModel(new File(path)).get

      val newMesh = model.sample

      def t(pt : Point[_3D]) : Point[_3D] = {
        val(refPt, ptId) = model.referenceMesh.findClosestPoint(pt)
        newMesh(ptId)
      }

      val newModel = model.changeReference(t)

      compareModels(model, newModel)
    }

//    it("can write a changed mean statistical mode, read it and still yield the same space") {
//      val tmpStatismoFile = File.createTempFile("statModel", ".h5")
//      tmpStatismoFile.deleteOnExit()
//
//      StatismoIO.writeStatismoMeshModel(newModel, tmpStatismoFile)
//      val readModel = StatismoIO.readStatismoMeshModel(tmpStatismoFile).get
//      compareModels(model, readModel)
//    }

  }

}