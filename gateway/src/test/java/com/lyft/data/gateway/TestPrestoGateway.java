package com.lyft.data.gateway;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestPrestoGateway {

  @BeforeClass(alwaysRun = true)
  public void setup() {
    // TODO: start presto docker container for 2 instances, once up, start presto-gw given the
    // port/host configs
  }

  @Test
  public void testMultipleQueries() {
    // TODO: implement this
  }

  @Test
  public void testMultipleQueriesUnderTransaction() {
    // TODO:
  }

  @AfterClass(alwaysRun = true)
  public void tearDown() {
    // TODO:
  }
}
