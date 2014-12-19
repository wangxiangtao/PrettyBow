package storage;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

public class JDBCConnectionWrapper {
	
	private Connection connection;
	private int selectLimit =  10000;
	private int commitLimit = 10000;
	
	public JDBCConnectionWrapper(String driver, String connectionString, String username, String password) 
		throws Throwable {
		Class.forName(driver);
		
		connection = DriverManager.getConnection(connectionString, username, password);
		connection.setAutoCommit(false);
	}
	
	public void setSelectLimit(int selectLimit) {
		ensureNotEmpty(selectLimit);
		this.selectLimit = selectLimit;
	}
	
	public void setCountLimit(int commitLimit) {
		ensureNotEmpty(commitLimit);
		this.commitLimit = commitLimit;
	}
	
	private void ensureNotEmpty(int value) {
		if(value <= 0) {
			throw new IllegalArgumentException("value cannot be 0 or less");
		}
	}
	
	public UpdateStatementWrapper update(String sql)
		throws Throwable {
		return new UpdateStatementWrapper(sql, commitLimit, connection);
	}
	
	public void update(String sql, Object ... parameters) 
		throws Throwable {
		UpdateStatementWrapper update = new UpdateStatementWrapper(sql, commitLimit, connection);

		for(int i = 0; i < parameters.length; i++) {
			update.setParameter(i, parameters[i]);
		}

		update.commit();
		update.close();
	}
	
	public SelectStatementWrapper select(String sql)
		throws Throwable {
		return new SelectStatementWrapper(sql, selectLimit, connection);
	}
	
	public class UpdateStatementWrapper {
		private Connection connection = null;
		private PreparedStatement updatePreparedStatement = null;
		
		private int commitLimit = 0;
		private int commitCount = 0;
		
		private UpdateStatementWrapper(String sql, int commitLimit, Connection connection) 
			throws Throwable {
			this.connection = connection;
			
			this.updatePreparedStatement = connection.prepareStatement(sql);
			this.commitLimit = commitLimit;
			this.commitCount = 0;
		}
		
		public void setParameter(int parameterIndex, Object object)
			throws Throwable {
			if(object instanceof String) {
				String tempString = (String)object;
				
				if(tempString != null) {
					StringBuilder stringBuilder = new StringBuilder();
					int length = tempString.length();
					int index = 0;
					
					while(index<length) {
						if(!Character.isHighSurrogate(tempString.charAt(index))) {
							stringBuilder.append(tempString.charAt(index));
						}
							index++;
					}
					
					tempString = stringBuilder.toString();
				}
				this.updatePreparedStatement.setString(parameterIndex, tempString);
			} else if(object instanceof Integer) {
				this.updatePreparedStatement.setInt(parameterIndex, (Integer)object);
			} else if(object instanceof Long) {
				this.updatePreparedStatement.setLong(parameterIndex, (Long)object);
			} else if(object instanceof Double) {
				this.updatePreparedStatement.setDouble(parameterIndex, (Double)object);
			} else if(object instanceof Float) {
				this.updatePreparedStatement.setFloat(parameterIndex, (Float)object);
			} else if(object instanceof Date) {
				this.updatePreparedStatement.setDate(parameterIndex, (Date)object);
			} else if(object instanceof Timestamp) {
				this.updatePreparedStatement.setTimestamp(parameterIndex, (Timestamp)object);
			} else if(object instanceof Array) {
				this.updatePreparedStatement.setArray(parameterIndex, (Array)object);
			} else if(object instanceof BigDecimal) {
				this.updatePreparedStatement.setBigDecimal(parameterIndex, (BigDecimal)object);
			} else if(object instanceof Byte) {
				this.updatePreparedStatement.setByte(parameterIndex, (Byte)object);
			} else if(object instanceof Blob) {
				this.updatePreparedStatement.setBlob(parameterIndex, (Blob)object);
			} else if(object instanceof Clob) {
				this.updatePreparedStatement.setClob(parameterIndex, (Clob)object);
			} else {
				this.updatePreparedStatement.setObject(parameterIndex, object);
			}
		}
		
		public void commit()
			throws Throwable {
			updatePreparedStatement.addBatch();
			commitCount++;
			
			if(commitCount >= commitLimit) {
				updatePreparedStatement.executeBatch();
				updatePreparedStatement.clearBatch();
				connection.commit();
				commitCount = 0;
			}
		}
		
		public void rollback()
			throws Throwable {
			connection.rollback();
		}
		
		public void close()
			throws Throwable {
			if(this.updatePreparedStatement != null) {
				this.updatePreparedStatement.executeBatch();
				this.updatePreparedStatement.clearBatch();
				this.updatePreparedStatement.close();
				this.updatePreparedStatement = null;
			}
			
			this.connection.commit();
		}
	}
	
	public class SelectStatementWrapper {
		private Statement selectStatement = null;
		private ResultSet resultSet = null;
		
		private int selectLimit = 0;
		private int fetchCount = 0;
		private int fetchIndex = 0;
		private String query = null;
		
		private SelectStatementWrapper(String sql, int selectLimit, Connection connection)
			throws Throwable {
			this.query = sql.trim();
			
			int startIndex = this.query.toUpperCase().indexOf("SELECT ");
			int endIndex = this.query.toUpperCase().indexOf(" FROM ");
			
			String countQuery = this.query.substring(0, startIndex + 7) + "COUNT(1)" + this.query.substring(endIndex);
			
			this.selectStatement = connection.createStatement();
			this.resultSet = this.selectStatement.executeQuery(countQuery);
			
			int total = resultSet.next() ? resultSet.getInt(1) : 0;		
			this.resultSet.close();
			this.resultSet = null;
			
			this.selectLimit = selectLimit;
			this.fetchCount = total > 0 ? (int)Math.ceil((total * 1.0)/ selectLimit) : 0;
			this.fetchIndex = 0;
		}
		
		public boolean next()
			throws Throwable {
			if(resultSet == null) {
				if(fetchCount > 0 && fetchIndex < fetchCount && query != null) {
					resultSet = selectStatement.executeQuery(query + " LIMIT " + (fetchIndex * selectLimit) + ", " + selectLimit);
					fetchIndex++;
				} else {
					return false;
				}
			}
			
			if(!resultSet.next()) {
				resultSet.close();
				
				if(fetchCount > 0 && fetchIndex < fetchCount && query != null) {
					resultSet = selectStatement.executeQuery(query + " LIMIT " + (fetchIndex * selectLimit) + ", " + selectLimit);
					fetchIndex++;

					return resultSet.next();
				} else {
					resultSet = null;
				
					return false;
				}
			} else {
				return true;
			}
		}
		
		public String getString(int columnIndex) 
			throws Throwable {
			return resultSet.getString(columnIndex);
		}
		
		public Integer getInteger(int columnIndex)
			throws Throwable {
			return resultSet.getInt(columnIndex);
		}
		
		public Long getLong(int columnIndex)
			throws Throwable {
			return resultSet.getLong(columnIndex);
		}
		
		public Double getDouble(int columnIndex)
			throws Throwable {
			return resultSet.getDouble(columnIndex);
		}
		
		public Float getFloat(int columnIndex)
			throws Throwable {
			return resultSet.getFloat(columnIndex);
		}
		
		public Date getDate(int columnIndex)
			throws Throwable {
			return resultSet.getDate(columnIndex);
		}
		
		public Timestamp getTimestamp(int columnIndex)
			throws Throwable {
			return resultSet.getTimestamp(columnIndex);
		}
		
		public Array getArray(int columnIndex)
			throws Throwable {
			return resultSet.getArray(columnIndex);
		}
		
		public BigDecimal getBigDecimal(int columnIndex)
			throws Throwable {
			return resultSet.getBigDecimal(columnIndex);
		}
		
		public Byte getByte(int columnIndex)
			throws Throwable {
			return resultSet.getByte(columnIndex);
		}
		
		public Blob getBlob(int columnIndex)
			throws Throwable {
			return resultSet.getBlob(columnIndex);
		}
		
		public Clob getClob(int columnIndex)
			throws Throwable {
			return resultSet.getClob(columnIndex);
		}
		
		public Object getObject(int columnIndex)
			throws Throwable {
			return resultSet.getObject(columnIndex);
		}
		
		
		public String getString(String columnName) 
			throws Throwable {
			return resultSet.getString(columnName);
		}
		
		public Integer getInteger(String columnName)
			throws Throwable {
			return resultSet.getInt(columnName);
		}
		
		public Long getLong(String columnName)
			throws Throwable {
			return resultSet.getLong(columnName);
		}
		
		public Double getDouble(String columnName)
			throws Throwable {
			return resultSet.getDouble(columnName);
		}
		
		public Float getFloat(String columnName)
			throws Throwable {
			return resultSet.getFloat(columnName);
		}
		
		public Date getDate(String columnName)
			throws Throwable {
			return resultSet.getDate(columnName);
		}
		
		public Timestamp getTimestamp(String columnName)
			throws Throwable {
			return resultSet.getTimestamp(columnName);
		}
		
		public Array getArray(String columnName)
			throws Throwable {
			return resultSet.getArray(columnName);
		}
		
		public BigDecimal getBigDecimal(String columnName)
			throws Throwable {
			return resultSet.getBigDecimal(columnName);
		}
		
		public Byte getByte(String columnName)
			throws Throwable {
			return resultSet.getByte(columnName);
		}
		
		public Blob getBlob(String columnName)
			throws Throwable {
			return resultSet.getBlob(columnName);
		}
		
		public Clob getClob(String columnName)
			throws Throwable {
			return resultSet.getClob(columnName);
		}
		
		public Object getObject(String columnName)
			throws Throwable {
			return resultSet.getObject(columnName);
		}
		
		public void close()
			throws Throwable {
			if(selectStatement != null) {
				selectStatement.close();
				selectStatement = null;
			}
			
			if(resultSet != null) {
				resultSet.close();
				resultSet = null;
			}
			
			query = null;
			fetchCount = 0;
			fetchIndex = 0;
		}
	} 
	
	public void close() 
		throws Throwable {
		connection.setAutoCommit(true);
		connection.close();
	}
}