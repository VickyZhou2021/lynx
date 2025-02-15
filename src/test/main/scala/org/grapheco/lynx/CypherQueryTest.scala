package org.grapheco.lynx

import org.grapheco.lynx.types.LynxValue
import org.grapheco.lynx.types.property.LynxString
import org.grapheco.lynx.types.structural.{LynxNode, LynxRelationship}
import org.junit.jupiter.api.{Assertions, BeforeEach, Test}

class CypherQueryTest extends TestBase {

  runOnDemoGraph(
    """
      |Create
      |(a:person:leader{name:"bluejoe", age: 40}),
      |(b:person{name:"alex", age: 30}),
      |(c{name:"CNIC", age: 10}),
      |(a)-[:knows]->(b),
      |(b)-[:knows]->(c),
      |(a)-[]->(c)
      |""".stripMargin)


  @Test
  def testQueryUnit(): Unit = {
    var rs: LynxResult = null
    rs = runOnDemoGraph("return 1")
    Assertions.assertEquals(Seq("1"), rs.columns)
    Assertions.assertEquals(1, rs.records().size)
    Assertions.assertEquals(LynxValue(1), rs.records().toSeq.apply(0)("1"))

    rs = runOnDemoGraph("return 1,2,3")
    Assertions.assertEquals(Seq("1", "2", "3"), rs.columns)
    Assertions.assertEquals(1, rs.records().size)
    Assertions.assertEquals(LynxValue(1), rs.records().toSeq.apply(0)("1"))
    Assertions.assertEquals(LynxValue(2), rs.records().toSeq.apply(0)("2"))
    Assertions.assertEquals(LynxValue(3), rs.records().toSeq.apply(0)("3"))

    rs = runOnDemoGraph("return 1+2")
    Assertions.assertEquals(Seq("1+2"), rs.columns)
    Assertions.assertEquals(1, rs.records().size)
    Assertions.assertEquals(LynxValue(3), rs.records().toSeq.apply(0)("1+2"))
  }

  @Test
  def testQueryUnitWithParams(): Unit = {
    var rs: LynxResult = null
    rs = runOnDemoGraph("return $N", Map("N" -> 1))
    Assertions.assertEquals(Seq("$N"), rs.columns)
    Assertions.assertEquals(1, rs.records().size)
    Assertions.assertEquals(LynxValue(1), rs.records().toSeq.apply(0)("$N"))
  }

  @Test
  def testQueryUnitAsN(): Unit = {
    val rs = runOnDemoGraph("return 1 as N")
    Assertions.assertEquals(Map("N" -> LynxValue(1)), rs.records.toSeq.apply(0))
    Assertions.assertEquals(LynxValue(1), rs.records.toSeq.apply(0)("N"))
  }

  @Test
  def testQueryWithUnit(): Unit = {
    val rs = runOnDemoGraph("with 1 as N return N")
    Assertions.assertEquals(Map("N" -> LynxValue(1)), rs.records.toSeq.apply(0))
    Assertions.assertEquals(LynxValue(1), rs.records.toSeq.apply(0)("N"))
  }

  @Test
  def testQueryNodes(): Unit = {
    val rs = runOnDemoGraph("match (n) return n")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(Seq(1.toLong, 2.toLong, 3.toLong), rs.records.toSeq.map(_.apply("n").asInstanceOf[LynxNode].id.value).toSeq)

  }

  @Test
  def testQueryWithLimit(): Unit = {
    var rs = runOnDemoGraph("match (n) return n limit 2")
    Assertions.assertEquals(2, rs.records.size)

    rs = runOnDemoGraph("match (n) return n limit 1")
    Assertions.assertEquals(1, rs.records.size)

    rs = runOnDemoGraph("match (n) return n limit 3")
    Assertions.assertEquals(3, rs.records.size)

    rs = runOnDemoGraph("match (n) return n limit 10")
    Assertions.assertEquals(3, rs.records.size)
  }

  @Test
  def testMatchWithReturn(): Unit = {
    val rs = runOnDemoGraph("match (n) with n.name as x, n.age as y return x,y")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(LynxValue("bluejoe"), rs.records.toSeq.apply(0).apply("x"))
    Assertions.assertEquals(LynxValue(40), rs.records.toSeq.apply(0).apply("y"))
    Assertions.assertEquals(LynxValue("alex"), rs.records.toSeq.apply(1).apply("x"))
    Assertions.assertEquals(LynxValue(30), rs.records.toSeq.apply(1).apply("y"))
    Assertions.assertEquals(LynxValue("CNIC"), rs.records.toSeq.apply(2).apply("x"))
    Assertions.assertEquals(LynxValue(10), rs.records.toSeq.apply(2).apply("y"))
  }

  @Test
  def testMatchWithReturnEval(): Unit = {
    val rs = runOnDemoGraph("match (n) return n.name,n.age+1")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(LynxValue(41), rs.records.toSeq.apply(0).apply("n.age+1"))
    Assertions.assertEquals(LynxValue(31), rs.records.toSeq.apply(1).apply("n.age+1"))
    Assertions.assertEquals(LynxValue(11), rs.records.toSeq.apply(2).apply("n.age+1"))
  }

  @Test
  def testMatchWhereWithReturn(): Unit = {
    var rs = runOnDemoGraph("match (n) where n.age>10 with n.name as x, n.age as y return x,y")
    Assertions.assertEquals(2, rs.records.size)
    Assertions.assertEquals(LynxValue("bluejoe"), rs.records.toSeq.apply(0).apply("x"))
    Assertions.assertEquals(LynxValue(40), rs.records.toSeq.apply(0).apply("y"))
    Assertions.assertEquals(LynxValue("alex"), rs.records.toSeq.apply(1).apply("x"))
    Assertions.assertEquals(LynxValue(30), rs.records.toSeq.apply(1).apply("y"))

    rs = runOnDemoGraph("match (n) where n.age>10 with n.name as x, n.age as y where y<40 return x,y")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(LynxValue("alex"), rs.records.toSeq.apply(0).apply("x"))
    Assertions.assertEquals(LynxValue(30), rs.records.toSeq.apply(0).apply("y"))
  }

  @Test
  def testQueryNamedRelations(): Unit = {
    var rs = runOnDemoGraph("match (m)-[r]-(n) return m,r,n")
    Assertions.assertEquals(6, rs.records.size)

    rs = runOnDemoGraph("match (m)-[r]->(n) return m,r,n")
    Assertions.assertEquals(3, rs.records.size)
    rs.records().foreach {
      map =>
        Assertions.assertEquals(map("r").asInstanceOf[LynxRelationship].startNodeId, map("m").asInstanceOf[LynxNode].id)
        Assertions.assertEquals(map("r").asInstanceOf[LynxRelationship].endNodeId, map("n").asInstanceOf[LynxNode].id)
    }

    rs = runOnDemoGraph("match (m)<-[r]-(n) return m,r,n")
    Assertions.assertEquals(3, rs.records.size)
    rs.records().foreach {
      map =>
        Assertions.assertEquals(map("r").asInstanceOf[LynxRelationship].startNodeId, map("n").asInstanceOf[LynxNode].id)
        Assertions.assertEquals(map("r").asInstanceOf[LynxRelationship].endNodeId, map("m").asInstanceOf[LynxNode].id)
    }

    rs = runOnDemoGraph("match (m)<-[r]->(n) return m,r,n")
    Assertions.assertEquals(6, rs.records.size)
  }

  @Test
  def testQueryPathTriple(): Unit = {
    var rs = runOnDemoGraph("match ()-[r]-() return r")
    Assertions.assertEquals(6, rs.records.size)

    rs = runOnDemoGraph("match ()-[r]->() return r")
    Assertions.assertEquals(3, rs.records.size)

    rs = runOnDemoGraph("match ()<-[r]-() return r")
    Assertions.assertEquals(3, rs.records.size)
  }

  @Test
  def testQuerySingleLongPath(): Unit = {
    var rs = runOnDemoGraph("match ()-[r]-()-[s]-() return r,s")
    Assertions.assertEquals(6, rs.records.size)

    rs = runOnDemoGraph("match ()-[r]->()-[s]-() return r,s")
    Assertions.assertEquals(3, rs.records.size)

    //(bluejoe)-[:KNOWS]->(alex)-[:KNOWS]->(CNIC)
    rs = runOnDemoGraph("match (m)-[r]->(n)-[s]->(x) return r,s")
    Assertions.assertEquals(1, rs.records.size)

    //(bluejoe)-[:KNOWS]->(alex)-[:KNOWS]->(CNIC)
    rs = runOnDemoGraph("match (m)-[r]->(n)-[s]->(x)<-[]-(m) return r,s")
    Assertions.assertEquals(1, rs.records.size)
  }

  @Test
  def testQueryMultipleMatchs(): Unit = {
    var rs = runOnDemoGraph("match ()-[r]-(n) match (n)-[s]-() return r,s")
    Assertions.assertEquals(6, rs.records.size)

    rs = runOnDemoGraph("match ()-[r]->(n) match (n)-[s]-() return r,s")
    Assertions.assertEquals(3, rs.records.size)

    rs = runOnDemoGraph("match (m)-[r]->(n) match (n)-[s]->(x) return r,s")
    Assertions.assertEquals(1, rs.records.size)

    rs = runOnDemoGraph("match (m)-[r]->(n) where m.age>18 match (n)-[s]->(x) where x.age<35 return r,s")
    Assertions.assertEquals(1, rs.records.size)
  }

  @Test
  def testQueryDistinctRelations(): Unit = {
    val rs = runOnDemoGraph("match (n)-[r]-(m) return distinct r")
    Assertions.assertEquals(3, rs.records.size)
  }

  @Test
  def testQueryMRN(): Unit = {
    val rs = runOnDemoGraph("match (m)-[r]->(n) return m,r,n")
    Assertions.assertEquals(3, rs.records.size)

    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testQueryNodeProperty(): Unit = {
    var rs = runOnDemoGraph("match (n) return 1,1+2,2>1,n,n.name")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(Seq("bluejoe", "alex", "CNIC"),
      rs.records().toSeq.map(_.apply("n.name").asInstanceOf[LynxString].value).toSeq)
    Assertions.assertEquals(LynxValue("bluejoe"), rs.records.toSeq.apply(0).apply("n.name"))
    Assertions.assertEquals(LynxValue(1), rs.records().toSeq.apply(0)("1"))
    Assertions.assertEquals(LynxValue(3), rs.records().toSeq.apply(0)("1+2"))
    Assertions.assertEquals(LynxValue(true), rs.records().toSeq.apply(0)("2>1"))

    rs = runOnDemoGraph("match (n) return 1,1+2,2>1 as v0,n,n.name as name")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(Seq("bluejoe", "alex", "CNIC"),
      rs.records().toSeq.map(_.apply("name").asInstanceOf[LynxString].value).toSeq)
    Assertions.assertEquals(LynxValue("bluejoe"), rs.records.toSeq.apply(0).apply("name"))
    Assertions.assertEquals(LynxValue(true), rs.records().toSeq.apply(0)("v0"))
  }

  @Test
  def testQueryNodePropertyAlias(): Unit = {
    val rs = runOnDemoGraph("match (n) return n.name as name")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(LynxValue("bluejoe"), rs.records.toSeq.apply(0).apply("name"))
  }

  @Test
  def testQueryNodesWithFilter(): Unit = {
    var rs = runOnDemoGraph("match (n) where n.name='bluejoe' return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n) where n.name=$name return n", Map("name" -> "bluejoe"))
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testQueryNodeWithLabels(): Unit = {
    var rs = runOnDemoGraph("match (n:person) return n")
    Assertions.assertEquals(2, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n:leader) return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n:person:leader) return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n:nonexisting) return n")
    Assertions.assertEquals(0, rs.records.size)
  }

  @Test
  def testQueryNodeWithProperties(): Unit = {
    var rs = runOnDemoGraph("match (n {name: 'bluejoe'}) return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n {name: 'CNIC'}) return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n {name: 'nonexisting'}) return n")
    Assertions.assertEquals(0, rs.records.size)

    rs = runOnDemoGraph("match (n:nonexisting {name: 'bluejoe'}) return n")
    Assertions.assertEquals(0, rs.records.size)

    rs = runOnDemoGraph("match (n:leader {name: 'bluejoe'}) return n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testQueryPathWithNodeLabel(): Unit = {
    var rs = runOnDemoGraph("match (n:person)-[r]->(m) return n,r,m")
    Assertions.assertEquals(3, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(1).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(2).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(2).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(2).apply("m").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n:person)-[r]->(m:person) return n,r,m")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testQueryPathWithNodeProperties(): Unit = {
    var rs = runOnDemoGraph("match (n {name:'bluejoe'})-[r]->(m) return n,r,m")
    Assertions.assertEquals(2, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(1).apply("r").asInstanceOf[LynxRelationship].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(1).apply("m").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (n:person {name:'bluejoe'})-[r]->(m:person) return n,r,m")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testQueryPathMN(): Unit = {
    var rs = runOnDemoGraph("match (m {name:'bluejoe'})-->(n) return m,n")
    Assertions.assertEquals(2, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(1).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(3.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (m:person)-->(n:person) return m,n")
    Assertions.assertEquals(1, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    rs = runOnDemoGraph("match (m:person)--(n:person) return m,n")
    Assertions.assertEquals(2, rs.records.size)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(0).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)

    Assertions.assertEquals(2.toLong, rs.records.toSeq.apply(1).apply("m").asInstanceOf[LynxNode].id.value)
    Assertions.assertEquals(1.toLong, rs.records.toSeq.apply(1).apply("n").asInstanceOf[LynxNode].id.value)
  }
}
