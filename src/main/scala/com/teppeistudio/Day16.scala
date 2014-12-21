package com.teppeistudio

import org.apache.spark._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.graphx.VertexRDD
import org.apache.spark.rdd.RDD

object Day16 {

    def main(args: Array[String]) = {

		val conf = new SparkConf()
		val sc = new SparkContext("local", "test", conf)

	    val graph:Graph[Int, Int]
	    	= GraphLoader.edgeListFile(sc, "graphdata/day16.tsv").cache()

	    // 綺麗にクラスタリングされたパターン
	    val graph1 = graph.mapVertices{ case(vid:Long, attr:Int) => if(vid < 4) 1 else 2 }

		println("\n\n~~~~~~~~~ Confirm Vertices Internal of graph1 ")
		graph1.vertices.collect.foreach(println(_))
		println("modurality : " + modurality(graph1))

	    // 変なクラスタリングされたパターン
	    val graph2 = graph.mapVertices{ case(vid:Long, attr:Int) => if(vid < 2) 1 else 2 }

		println("\n\n~~~~~~~~~ Confirm Vertices Internal of graph2 ")
		graph2.vertices.collect.foreach(println(_))
		println("modurality : " + modurality(graph2))

	    // クラスタがひとつだけになってしまっているパターン
	    val graph3 = graph.mapVertices{ case(vid:Long, attr:Int) => 1 }

		println("\n\n~~~~~~~~~ Confirm Vertices Internal of graph3 ")
		graph3.vertices.collect.foreach(println(_))
		println("modurality : " + modurality(graph3))

	    // ひとつの頂点にひとつのクラスタが割り当たっているパターン
	    val graph4 = graph.mapVertices{ case(vid:Long, attr:Int) => vid.toInt }

		println("\n\n~~~~~~~~~ Confirm Vertices Internal of graph4 ")
		graph4.vertices.collect.foreach(println(_))
		println("modurality : " + modurality(graph4))

	    // ３つにクラスタされたパターン
	    val graph5 = graph.mapVertices{ case(vid:Long, attr:Int) => 
	    	if(vid < 3) 1 
	    	else if(vid < 5) 2
	    	else 3 
	    }
		graph5.vertices.collect.foreach(println(_))
		println("modurality : " + modurality(graph5))


		sc.stop
	}

	def modurality(graph:Graph[Int, Int]):Float = {
		val edgesCnt:Float = graph.edges.count.toFloat

		println("--- edgesCnt : " + edgesCnt)

		val clusters:Array[Int] = graph.vertices.map(v => (v._2,1)).groupBy(g => g._1).map(g => g._1).collect

		def edgesCntOfCluster(graph:Graph[Int, Int], cluster:Int):Float = {
			graph.subgraph(vpred = (vid, attr) => attr == cluster).edges.count.toFloat
		}

		def edgesCntBetweenClusters(graph:Graph[Int, Int], clusterA:Int, clusterB:Int):Float = {
			graph.subgraph(epred = { edge => 
				if(edge.srcAttr == clusterA && edge.dstAttr == clusterB) true
				else if(edge.srcAttr == clusterB && edge.dstAttr == clusterA) true
				else false
			}).edges.count.toFloat
		}

		var mod:Float = 0.0F
		for( c1 <- clusters ){
			val ecoc = edgesCntOfCluster(graph, c1)
			println("------ edgesCntOfCluster : " + ecoc)
			val edgesCntOfClusterRate:Float =  ecoc / edgesCnt
			println("------ edgesCntOfClusterRate : " + edgesCntOfClusterRate)

			var edgesCntBetweenClustersRateSum:Float = 0.0F
			for( c2 <- clusters ){
				if(c1 != c2) {
					val ecbc = edgesCntBetweenClusters(graph, c1, c2)
					println("--------- edgesCntBetweenClusters : " + ecbc + "(" + c1 + " - " + c2 + ")")
					val edgesCntBetweenClustersRate:Float =  ecbc / edgesCnt
					println("--------- edgesCntBetweenClustersRate : " + edgesCntBetweenClustersRate)
					edgesCntBetweenClustersRateSum = edgesCntBetweenClustersRateSum + edgesCntBetweenClustersRate
				}
			}
			println("------ edgesCntBetweenClustersRateSum : " + edgesCntBetweenClustersRateSum)

			mod = mod + (edgesCntOfClusterRate - edgesCntBetweenClustersRateSum * edgesCntBetweenClustersRateSum)
		}
		println("--- mod : " + mod )

		return mod
	}
}

