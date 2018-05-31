package org.postgresql.test.jdbc2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.postgresql.PGProperty;
import org.postgresql.core.ServerVersion;
import org.postgresql.jdbc.AutoSave;
import org.postgresql.test.TestUtil;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This test case verifies the functionality of committing each entry in the batch as they are executed provided that
 * autosave=always
 * Current implementation uses SAVEPOINTS to achieve this functionality while the new functionality will use a server
 * side feature currently being implemented to achieve the same functionality
 */

public class BatchAutoSaveTest {

  private String tableName = "batch_transactions";
  private Connection defaultConnection = null;

  @Before
  public void setUp() throws Exception {
    defaultConnection = TestUtil.openDB();
    TestUtil.createTable(defaultConnection, tableName,
        "id serial primary key CHECK (id NOT IN (3, 7)), name text");
  }

  @After
  public void tearDown() throws Exception {
    if (defaultConnection != null) {
      TestUtil.dropTable(defaultConnection, tableName);
      TestUtil.closeDB(defaultConnection);
    }
  }

  /**
   * When autocommit is on, autosave is off
   * Fail the whole batch as there is no savepoint expected. This would keep the old behaviour
   */
  @Test
  public void testAutoCommitNoAutoSave() throws Exception {
    Connection con = TestUtil.openDB();
    Statement stmt = con.createStatement();
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on duplicate key");
    } catch (BatchUpdateException be) {
      TestUtil.assertNumberOfRows(con, tableName, 0, "Expected no rows in table");
      int[] ret = be.getUpdateCounts();
      Assert.assertEquals("Failed to match batch update count",
          Arrays.toString(new int[]{-3, -3, -3, -3}), Arrays.toString(ret));
    } finally {
      TestUtil.closeDB(con);
    }
  }

  /**
   * When autocommit is off, autosave is off
   * No rows will be committed, keep the old behaviour
   */
  @Test
  public void testNotAutoSaveTransaction() throws Exception {
    Connection con = TestUtil.openDB();
    con.setAutoCommit(false);
    Statement stmt = con.createStatement();
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on duplicate key");
    } catch (BatchUpdateException be) {
      con.rollback();
      expectRows(con, be, 0, new int[]{-3, -3, -3, -3});
    } finally {
      TestUtil.closeDB(con);
    }
  }

  @Test
  public void testAutoCommitAutoSaveALWAYS() throws Exception {
    testAutoCommitAutoSave(AutoSave.ALWAYS);
  }

  /*
   * Ensures that autosave=server works the same as autosave=always, if supported, or
   * fails with the correct exception if unsupported.
   */
  @Test
  public void testAutoCommitAutoSaveSERVER() throws Exception {
    try {
      testAutoCommitAutoSave(AutoSave.SERVER);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }
  }

  @Test
  public void testTransactionAutoSaveALWAYS() throws Exception {
    testTransactionAutoSave(AutoSave.ALWAYS, AutoSave.ALWAYS);
  }

  @Test
  public void testTransactionAutoSaveSERVER() throws Exception {
    try {
      testTransactionAutoSave(AutoSave.SERVER, AutoSave.SERVER);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }
  }

  @Test
  public void testTransactionAutoSaveSERVERSwitch() throws Exception {
    try {
      testTransactionAutoSave(AutoSave.ALWAYS, AutoSave.SERVER);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }

    try {
      testTransactionAutoSave(AutoSave.SERVER, AutoSave.ALWAYS);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }
  }

  @Test
  public void testGeneratedKeysAutoCommitAutoSaveALWAYS() throws Exception {
    testGeneratedKeysAutoCommitAutoSave(AutoSave.ALWAYS);
  }

  @Test
  public void testGeneratedKeysAutoCommitAutoSaveSERVER() throws Exception {
    try {
      testGeneratedKeysAutoCommitAutoSave(AutoSave.SERVER);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }
  }

  @Test
  public void testGeneratedKeysTransactionAutoSaveALWAYS() throws Exception {
    testGeneratedKeysTransactionAutoSave(AutoSave.ALWAYS);
  }

  @Test
  public void testGeneratedKeysTransactionAutoSaveSERVER() throws Exception {
    try {
      testGeneratedKeysTransactionAutoSave(AutoSave.SERVER);
      Assert.assertTrue("Should have failed as autoSave=SERVER is not supported on this server",
          TestUtil.haveMinimumServerVersion(defaultConnection, ServerVersion.v11));
    } catch(Exception exp) {
      Assert.assertFalse("Failed to connect using autoSave=SERVER\n"+getReason(exp),
          !transactionRollbackSupportError(exp));
    }
  }

  /**
   * When autocommit is on, autosave is on
   * 1st two rows should have committed while the third row should be
   * rolled back
   * Fourth row will be committed however
   * AutoGeneratedKeys should contains resultset of three rows
   */
  private void testGeneratedKeysAutoCommitAutoSave(AutoSave autoSave) throws Exception {
    if (autoSave != AutoSave.ALWAYS && autoSave != AutoSave.SERVER) {
      throw new IllegalArgumentException("Allowed values are ALWAYS, SERVER");
    }

    Connection con = getConnection(autoSave, true);

    PreparedStatement stmt = con.prepareStatement("insert into " + tableName + "(name) values (?)",
        Statement.RETURN_GENERATED_KEYS);
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on check constraint");
    } catch (BatchUpdateException be) {
      List<Integer> items = new ArrayList<Integer>();

      ResultSet rs = stmt.getGeneratedKeys();
      while (rs.next()) {
        items.add(rs.getInt(1));
      }
      Assert.assertEquals(3, items.size()); // size of autogenerated keys
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
    } finally {
      con.close();
    }
  }

  /**
   * When autocommit is off, autosave is on
   * 1st two rows should have committed while the third row should be
   * rolled back
   * Fourth row will be committed however
   * AutoGeneratedKeys should contains resultset of three rows
   */
  private void testGeneratedKeysTransactionAutoSave(AutoSave autoSave) throws Exception {
    if (autoSave != AutoSave.ALWAYS && autoSave != AutoSave.SERVER) {
      throw new IllegalArgumentException("Allowed values are ALWAYS, SERVER");
    }

    Connection con = getConnection(autoSave, false);

    PreparedStatement stmt = con.prepareStatement("insert into " + tableName + "(name) values (?)",
        Statement.RETURN_GENERATED_KEYS);
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on check constraint");
    } catch (BatchUpdateException be) {
      List<Integer> items = new ArrayList<Integer>();

      ResultSet rs = stmt.getGeneratedKeys();
      while (rs.next()) {
        items.add(rs.getInt(1));
      }
      Assert.assertEquals(3, items.size()); // size of autogenerated keys
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
      con.rollback();
      TestUtil.assertNumberOfRows(con, tableName, 0, "Expected no rows in table");
    } finally {
      TestUtil.closeDB(con);
    }

    con = getConnection(autoSave, false);
    stmt = con.prepareStatement("insert into " + tableName + "(name) values (?)",
        Statement.RETURN_GENERATED_KEYS);
    prepareBatch(stmt);

    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on check constraint");
    } catch (BatchUpdateException be) {
      List<Integer> items = new ArrayList<Integer>();

      ResultSet rs = stmt.getGeneratedKeys();
      while (rs.next()) {
        items.add(rs.getInt(1));
      }
      Assert.assertEquals(3, items.size()); // size of autogenerated keys
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
      con.commit();
      TestUtil.assertNumberOfRows(con, tableName, 3, "Expected three rows in table");
    } finally {
      TestUtil.closeDB(con);
    }

  }

  /**
   * When autocommit is on, autosave is on
   * 1st two rows should have committed while the third row should be
   * rolled back
   * Fourth row will be committed however
   */
  private void testAutoCommitAutoSave(AutoSave autoSave) throws Exception {
    if (autoSave != AutoSave.ALWAYS && autoSave != AutoSave.SERVER) {
      throw new IllegalArgumentException("Allowed values are ALWAYS, SERVER");
    }

    Connection con = getConnection(autoSave, true);
    Statement stmt = con.createStatement();
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on duplicate key");
    } catch (BatchUpdateException be) {
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
    } finally {
      TestUtil.closeDB(con);
    }
  }

  /**
   * When autocommit is off, autosave is on
   * We need to sync after each statement however they will not be
   * committed by the current server
   *
   * 1st two rows should have committed while the third row should be
   * rolled back
   * Fourth row will be committed however
   *
   * Allow setting of autosave=server at connect-time or at runtime for the 2nd test, so
   * we can validate that we switch modes correctly. Accordingly we re-use the session
   * for both jobs.
   */
  private void testTransactionAutoSave(AutoSave autoSaveConnect, AutoSave autoSaveRuntime) throws Exception {
    if (autoSaveConnect != AutoSave.ALWAYS && autoSaveConnect != AutoSave.SERVER) {
      throw new IllegalArgumentException("Allowed values are ALWAYS, SERVER");
    }

    Connection con = getConnection(autoSaveConnect, false);
    org.postgresql.PGConnection pgcon = con.unwrap(org.postgresql.PGConnection.class);
    Assert.assertEquals("autosave mode not reported correctly after assignment",
      autoSaveConnect, pgcon.getAutosave());

    Statement stmt = con.createStatement();
    stmt.execute("DELETE FROM " + tableName);
    con.commit();

    stmt = con.createStatement();
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on duplicate key");
    } catch (BatchUpdateException be) {
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
      con.rollback();
      TestUtil.assertNumberOfRows(con, tableName, 0, "Expected no rows in table");
    } finally {
      con.rollback();
    }

    pgcon.setAutosave(autoSaveRuntime);
    Assert.assertEquals("autosave mode not reported correctly after assignment",
      autoSaveRuntime, pgcon.getAutosave());

    stmt = con.createStatement();
    prepareBatch(stmt);
    try {
      stmt.executeBatch();
      Assert.fail("Should have failed on duplicate key");
    } catch (BatchUpdateException be) {
      expectRows(con, be, 3, new int[]{1, 1, -3, 1});
      con.commit();
      TestUtil.assertNumberOfRows(con, tableName, 3, "Expected three rows in table");
    } finally {
      con.commit();
      TestUtil.closeDB(con);
    }
  }

  // Util methods
  private void prepareBatch(Statement stmt) throws SQLException {
    stmt.addBatch(TestUtil.insertSQL(tableName, "1, 'Test1'"));
    stmt.addBatch(TestUtil.insertSQL(tableName, "2, 'Test2'"));
    stmt.addBatch(TestUtil.insertSQL(tableName, "3, 'Test3'"));
    stmt.addBatch(TestUtil.insertSQL(tableName, "4, 'Test4'"));
  }

  private void prepareBatch(PreparedStatement stmt) throws SQLException {
    stmt.setString(1, "Test1");
    stmt.addBatch();

    stmt.setString(1, "Test2");
    stmt.addBatch();

    stmt.setString(1, "Test22");
    stmt.addBatch();

    stmt.setString(1, "Test3");
    stmt.addBatch();
  }

  private void expectRows(Connection con, BatchUpdateException be, int expectedRowCount,
      int[] expectedUpdateCounts) throws Exception {
    TestUtil.assertNumberOfRows(con, tableName, expectedRowCount,
        String.format("Expected {0} rows in table", expectedRowCount));
    int[] ret = be.getUpdateCounts();
    Assert.assertEquals("Failed to match batch update count",
        Arrays.toString(expectedUpdateCounts), Arrays.toString(ret));
  }

  private Connection getConnection(AutoSave autoSave, boolean autoCommit) throws Exception {
    Properties props = new Properties();
    PGProperty.AUTOSAVE.set(props, autoSave.value());
    Connection con = TestUtil.openDB(props);
    con.setAutoCommit(autoCommit);
    return con;
  }

  private String getReason(Throwable exp) {
    if (exp.getCause() != null) {
      exp = exp.getCause();
    }

    return exp.getMessage();
  }

  private boolean transactionRollbackSupportError(Throwable exp) {
    if (exp.getCause() != null) {
      exp = exp.getCause();
    }

    ServerErrorMessage errorMsg = ((PSQLException)exp).getServerErrorMessage();
    if (errorMsg != null) {
      return errorMsg.getSQLState().equals(PSQLState.UNDEFINED_OBJECT.getState()) && errorMsg.getMessage()
          .contains("transaction_rollback_scope");
    }

    return false;
  }
}
