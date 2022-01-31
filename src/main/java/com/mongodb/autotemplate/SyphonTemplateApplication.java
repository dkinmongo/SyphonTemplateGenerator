package com.mongodb.autotemplate;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.text.SimpleDateFormat;
import java.sql.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class SyphonTemplateApplication {
	@Autowired
	private Environment env;

	private static Logger logger = LoggerFactory.getLogger(SyphonTemplateApplication.class);
	private final static SimpleDateFormat stdDate = new SimpleDateFormat("yyMM");
	private static Connection conn;

	private static String SOURCE_URL;
	private static String SOURCE_USER;
	private static String SOURCE_PWD;
	private static String OWNER;  // oracle dedicated
	private static String SOURCE_DB_NAME; // mysql dedicated
	private static String[] SOURCE_TABLES;

	private static String MONGO_URL;
	private static String MONGO_DB_NAME;

	private static int select_dbms;    // 1: oracle, 2: mysql


	private static void genTemplate(String tableName, int db) {
		/*
		db  1:oracle, 2:mysql
		 */
		try {
			StringBuilder sqlString = null;
			Document sourceSource = null;

			if (db==1){  // oracle
				sqlString = new StringBuilder()
						.append("SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS where owner='" + OWNER + "' and TABLE_NAME='").append(tableName).append("'");
				sourceSource =
						new Document("uri", SOURCE_URL)
								.append("user", SOURCE_USER)
								.append("password", SOURCE_PWD);
			}else if(db==2){  // mysql
				sqlString = new StringBuilder()
						.append("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='" + SOURCE_DB_NAME + "' AND TABLE_NAME='").append(tableName).append("' order by ordinal_position;");
				sourceSource =
						new Document("uri", SOURCE_URL)
								.append("user", SOURCE_USER)
								.append("password", SOURCE_PWD);
			}
			PreparedStatement psmt = conn.prepareStatement(sqlString.toString());
			ResultSet rs = psmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			String value = null;

			Document targetSource =
					new Document("mode", "insert")
					.append("uri", MONGO_URL)
					.append("namespace", MONGO_DB_NAME + "."+ tableName);
			Document templateSource = new Document();
			Document querySource =
					new Document("sql", "SELECT * FROM "+tableName);

			while (rs.next()) {
				value = rs.getObject(1).toString();
				templateSource.append(value, "$" + value);
			}
			Document startSource =
					new Document("source", sourceSource)
					.append("target", targetSource)
					.append("template", templateSource)
					.append("query", querySource);
			Document startDoc = new Document("start", startSource);
			//logger.info(startDoc.toJson());
			System.out.println(startDoc.toJson());


		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
	}

	public static void generateTemplateForSyphon(int db) {
		/*
		1: oracle, 2: mysql
		 */
		try {
			for (String tableName : SOURCE_TABLES) {
				genTemplate( tableName, db );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initialize_mysql(){
		String driver = "com.mysql.cj.jdbc.Driver";

		try {
			conn = getConnection(driver, SOURCE_URL, SOURCE_USER, SOURCE_PWD);
		} catch(Exception e){
			logger.error("Exception due to " + e);
		}
	}

	public static void initialize_oracle(){
		// ---------------------------------------------------------------------
		// Oracle Connect
		String driver = "oracle.jdbc.driver.OracleDriver";
		try {
			conn = getConnection(driver, SOURCE_URL, SOURCE_USER, SOURCE_PWD);
		} catch(Exception e){
			logger.error("Exception due to " + e);
		}

		// POJO Object를 등록
		CodecRegistry pojoCodecRegistry = fromProviders(
				PojoCodecProvider.
						builder().
						// Add Custom POJO Packages
						//register("").
						build()
		);
	}

	public static Connection getConnection(String driver, String url, String user, String pwd) {
		Connection conn = null;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, pwd);
		} catch (SQLException se) {
			String msg = se.getMessage();
			logger.error(msg, se);
			se.printStackTrace();
		} catch (Exception e) {
			String msg = e.getMessage();
			logger.error(msg, e);
			e.printStackTrace();;
		}
		return conn;
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			if (env.getProperty("source.dbms").equals("mysql")) {
				select_dbms = 2;
				SOURCE_DB_NAME = env.getProperty("source.dbname");
			}
			else if (env.getProperty("source.dbms").equals("oracle")) {
				select_dbms = 1;
				OWNER = env.getProperty("source.owner");
			}
			else
				logger.error("Please enter a correct source dbms: [mysql|oracle]. Other dbms is not supported."+ "["+ env.getProperty("source.dbms") + "]");


			SOURCE_URL=env.getProperty("source.uri");
			SOURCE_USER=env.getProperty("source.username");
			SOURCE_PWD=env.getProperty("source.passwd");
			SOURCE_TABLES=env.getProperty("source.tablename").split(",");

			if (!env.getProperty("target.dbms").equals("MongoDB"))
				logger.error("Please enter a correct target dbms: [MongoDB]. Other dbms is not supported." + "["+ env.getProperty("target.dbms") + "]");

			MONGO_URL=env.getProperty("target.uri");
			MONGO_DB_NAME=env.getProperty("target.dbname");

			if (select_dbms == 1){
				// oracle
				initialize_oracle();   // make connections for oracle (1)
				generateTemplateForSyphon(1);
			} else if (select_dbms == 2) {
				initialize_mysql();    // make connection for mysql   (2)
				generateTemplateForSyphon(2);
			}
			conn.close();
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SyphonTemplateApplication.class, args);
	}

}