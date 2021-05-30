package com.mongodb.autotemplate;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import java.text.SimpleDateFormat;
import java.sql.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class SyphonTemplateApplication {
	private final static String[] tableList = {"owner","pet","species"};

	private static Logger logger = LoggerFactory.getLogger(SyphonTemplateApplication.class);
	private final static SimpleDateFormat stdDate = new SimpleDateFormat("yyMM");
	private static Connection conn;

	private static void genTemplate(String tableName, int db) {
		/*
		db  1:oracle, 2:mysql
		 */
		try {
			StringBuilder sqlString = null;
			Document sourceSource = null;

			if (db==1){  // oracle
				sqlString = new StringBuilder()
						.append("SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS where owner='TEST' and TABLE_NAME='").append(tableName).append("'");
				sourceSource =
						new Document("uri", "jdbc:oracle:thin:@localhost:1521/orclpdb1.localdomain")
								.append("user", "test")
								.append("password", "test");
			}else if(db==2){  // mysql
				sqlString = new StringBuilder()
						.append("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='sdemo' AND TABLE_NAME='").append(tableName).append("' order by ordinal_position;");
				sourceSource =
						new Document("uri", "jdbc:mysql://localhost:3306/sdemo?useSSL=false")
								.append("user", "root")
								.append("password", "manager");
			}
			PreparedStatement psmt = conn.prepareStatement(sqlString.toString());
			ResultSet rs = psmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			String value = null;

			Document targetSource =
					new Document("mode", "insert")
					.append("uri", "mongodb://mongoadmin:passwordone@localhost:30000,localhost:30001,localhost:30002/")
					.append("namespace", "test."+ tableName);
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
			logger.info(startDoc.toJson());

		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
	}

	public static void generateTemplateForSyphon(int db) {
		/*
		1: oracle, 2: mysql
		 */
		try {
			for (String tableName : tableList) {
				genTemplate( tableName, db );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initialize_mysql(){
		String driver = "com.mysql.cj.jdbc.Driver";
		String url="jdbc:mysql://localhost:3306/sdemo?useSSL=false";
		String user="root";
		String pwd="manager";

		try {
			conn = getConnection(driver, url, user, pwd);
		} catch(Exception e){
			System.out.println("Exception due to " + e);
		}
		System.out.println("conn: " + conn);
	}

	public static void initialize_oracle(){
		// ---------------------------------------------------------------------
		// Oracle Connect
		String driver = "oracle.jdbc.driver.OracleDriver";
		String url="jdbc:oracle:thin:@localhost:1521/orclpdb1.localdomain";
		String user="test";
		String pwd="test";

		try {
			conn = getConnection(driver, url, user, pwd);
		} catch(Exception e){
			System.out.println("Exception due to " + e);
		}
		System.out.println("conn: " + conn);

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
			//initialize_oracle();   // make connections for oracle (1)
			initialize_mysql();    // make connection for mysql   (2)
			generateTemplateForSyphon(2);
			conn.close();
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SyphonTemplateApplication.class, args);
	}

}