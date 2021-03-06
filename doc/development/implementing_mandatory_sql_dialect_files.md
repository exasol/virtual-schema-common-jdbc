# Implementing the SQL Dialect Adapter's Mandatory Files 

Each SQL Dialect adapter consists of two or more classes, depending on how standard-compliant the source behaves.
The minimum implementation requires that you create the following mandatory files:  
  
1. [`<Your_dialect_name>SqlDialect.java`](#creating-main-dialect-class)
1. [`<Your_dialect_name>SqlDialectFactory.java`](#creating-the-sql-dialect-factory)
1. [/src/main/resources/META-INF/services/com.exasol.adapter.dialects.SqlDialectFactory](#making-the-sql-dialect-factory-available-for-loading)

See Athena dialect source code in [Athena Virtual Schema Repository](https://github.com/exasol/athena-virtual-schema).

And we also need to create corresponding test classes.

## Creating Main Dialect Class

1. Start by **creating a new repository** called with the new dialect name, for example `athena-virtual-schema`. The packages structure:  
    `athena-virtual-schema/src/main/java/com/exasol/adapter/dialects/athena` 

    And one more **package for tests**:  
   `athena-virtual-schema/src/test/java/com/exasol/adapter/dialects/athena`
   
1. Add the [`virtual-schema-common-jdbc`](https://search.maven.org/search?q=virtual-schema-common-jdbc) package as a dependency to the `pom.xml` file.

1. Now **create a stub class for the dialect**: `com.exasol.adapter.dialects.athena.AthenaSqlDialect` that **extends** `AbstractDialect`. 
    Let your IDE generate necessary **overriding methods** to remove all compilation exceptions. You will implement them later:
   
    ```java
   public class AthenaSqlDialect extends AbstractSqlDialect {
        @Override
        public String getName() {
            return null;
        }
       
        @Override
        public Capabilities getCapabilities() {
            return null;
        }
   
       //other methods here
   } 
   ```

1. **Add a constructor** that takes a [JDBC connection](https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html) factory and user properties as parameters. 
    Or let your IDE generate it for you. You might wonder, why you get a factory for a connection instead of a connection. 
    This allows your dialect to decide if and when you really need a connection to the remote data source. 
    Depending on that source, establishing a connection can be costly, so having full control over connection creation can safe resources and execution time.
  
    The constructor of an `AbstractSqlDialect` also takes a set of dialect-specific properties. 
    A set of pre-defined properties which each dialect must support you can find inside the `AbstractSqlDialect`. You don't need to re-define them.
   
    ```java
    /**
     * Create a new instance of the {@link AthenaSqlDialect}.
     *
     * @param connectionFactory factory for the JDBC connection to the remote data source
     * @param properties        user-defined adapter properties
     */
    public AthenaSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        super(connectionFactory, properties, Set.of(CATALOG_NAME_PROPERTY, SCHEMA_NAME_PROPERTY));
    }
    ```
   
1. **Add a `NAME` constant** and **implement method `getName()`** to report the name of the dialect:

    ```java
    static final String NAME = "ATHENA";

    public static String getName() {
       return NAME;
    }
    ```

    The constant `NAME` is package-scoped because we also need it in a method of the [factory](#creating-the-sql-dialect-factory) that instantiates the dialect _before_ an instance is available.

1. **Add a `CAPABILITIES` constant  and `createCapabilityList()` method** that returns NULL.

    Since the capabilities of the adapter do not change at runtime, we assigned them to a constant. 
    This way the `Capabilities` object is instantiated only once which makes querying the capabilities cheaper.

    Also **implement `getCapabilities()` method** returning the `CAPABILITIES` constant you've just created.

   ```java
   private static final Capabilities CAPABILITIES = createCapabilityList();
    
   private static Capabilities createCapabilityList() {
       return null;   
   }
   
   @Override
   public Capabilities getCapabilities() {
       return CAPABILITIES;
   }
   ```
  
1. Create a **new unit test class** for the dialect in the test package you created before: `com.exasol.adapter.dialects.athena.AthenaSqlDialectTest` that tests class `AthenaSqlDialect`.

    ### Acquiring Information About the Specifics of the Dialect
    
    Now that you have the skeleton of the dialect adapter, it is time to implement the specifics.
    
    There are three ways to find out, what the specifics of the source that you want to attach to are. 
    Which of those works depends largely on the availability and quality of the available information about that data source.
    
    They are listed in ascending order of effort you need to spend:
    
    1. Read the documentation
    2. Read the source code
    3. Reverse engineering
    
    In most cases it is at least a combination of 1. and 3. If you are lucky enough to attach to an Open Source product, 2. is incredibly helpful.
    
    In our Athena example, the [user guide](https://docs.aws.amazon.com/athena/latest/ug/what-is.html) is a good starting point for investigating capabilities, data mapping and special behavior.

    ### Defining the Supported Capabilities
    
    First you need to find out, which capabilities the source supports and looking at the list of [SQL queries, functions and operators](https://docs.aws.amazon.com/athena/latest/ug/functions-operators-reference-section.html) contains what you need to assemble the capability list. 
    All you have to do is read through the SQL reference and each time you come across a capability, mark that to the list.
    
    The list of capabilities that Exasol's Virtual Schemas know can be found in `com.exasol.adapter.capabilities` package from the project [`virtual-schema-common-java`](https://github.com/exasol/virtual-schema-common-java). 
    If you look at the JavaDoc of that class, you find helpful examples of what that capability means.
    
    You only need to pick up **the capabilities from the existing lists** in the common part of the project. 
    If the source supports something that is not in our lists, you can ignore it.
    
1. **Write a unit test** that checks whether the SQL dialect adapter **reports the capabilities** that you find in the documentation of the data source.
    We have 5 types of capabilities: 
    
     1. Main Capabilities
     2. Literal Capabilities
     3. Predicate Capabilities
     4. Scalar Function Capabilities
     5. Aggregate Function Capabilities
    
    Let's start from the **Main Capabilities** test. Here is an example from the Athena adapter.

    ```java
   package com.exasol.adapter.dialects.athena;
   
   import static com.exasol.adapter.AdapterProperties.*;
   import static com.exasol.adapter.capabilities.MainCapability.*;
   import static org.hamcrest.Matchers.*;
   import static org.hamcrest.MatcherAssert.assertThat;
      
   import org.junit.jupiter.api.BeforeEach;
   import org.junit.jupiter.api.Test;
   import org.junit.jupiter.api.extension.ExtendWith;
   import org.mockito.Mock;
   import org.mockito.junit.jupiter.MockitoExtension;
   
   import com.exasol.adapter.AdapterProperties;
   import com.exasol.adapter.jdbc.ConnectionFactory;
   
   @ExtendWith(MockitoExtension.class)
   class AthenaSqlDialectTest {
       private AthenaSqlDialect dialect;
       @Mock
       private ConnectionFactory connectionFactoryMock;
   
       @BeforeEach
       void beforeEach() {
           this.dialect = new AthenaSqlDialect(this.connectionFactoryMock, AdapterProperties.emptyProperties());
       }
    
        @Test
        void testGetMainCapabilities() {
            assertThat(this.dialect.getCapabilities().getMainCapabilities(),
                    containsInAnyOrder(SELECTLIST_PROJECTION, SELECTLIST_EXPRESSIONS, FILTER_EXPRESSIONS,
                            AGGREGATE_SINGLE_GROUP, AGGREGATE_GROUP_BY_COLUMN, AGGREGATE_GROUP_BY_EXPRESSION,
                            AGGREGATE_GROUP_BY_TUPLE, AGGREGATE_HAVING, ORDER_BY_COLUMN, ORDER_BY_EXPRESSION, LIMIT));
        }
    }
    ```

    Reading through the Athena and Presto documentation you will realize that while `LIMIT` in general is supported `LIMIT_WITH_OFFSET` is not. 
    The unit test reflects that.

    Run the test, and it must fail, since you did not implement the capability reporting method yet.

1. Now **implement the main capabilities part of the `createCapabilityList()` method** in the `<Your_dialect_name>SqlDialect.java` class so that it returns the main capabilities you added to the test.
    We use a builder and an `addMain()` method to add capabilities. 

    There are also `addLiteral()`, `addPredicate()`, `addScalarFunction()`, and `addAggregateFunction()` methods for other kinds of capabilities.
   
    ```java
    private static Capabilities createCapabilityList() {
        return Capabilities //
                .builder() //
                .addMain(SELECTLIST_PROJECTION, SELECTLIST_EXPRESSIONS, FILTER_EXPRESSIONS, AGGREGATE_SINGLE_GROUP,
                         AGGREGATE_GROUP_BY_COLUMN, AGGREGATE_GROUP_BY_EXPRESSION, AGGREGATE_GROUP_BY_TUPLE,
                         AGGREGATE_HAVING, ORDER_BY_COLUMN, ORDER_BY_EXPRESSION, LIMIT) //
                //here will be other methods. For example, `addLiteral()`
                .build();
    }
    ```

1. Repeat the previous two steps for all **other kinds of capabilities**.

    ### Defining Catalog and Schema Support

    Some databases know the concept of catalogs, others don't. Sometimes databases simulate a single catalog. 
    The same is true for schemas. In case of a relational database you can try to find out whether catalogs and / or schemas are supported by simply looking at the Data Definition Language (DDL) statements that the SQL dialect provides. 
    If `CREATE SCHEMA` exists, the database supports schemas.

    If on the other hand those DDL commands are missing, that does not rule out that pseudo-catalogs and schemas are used. You will see why shortly.

    A Virtual Schema needs to know how the data source handles catalogs and schemas, so that it can:
    1. Validate user-defined catalog and schema properties.
    2. Apply catalog and schema filters only where those concepts are supported.

    A quick look at the [Athena DDL](https://docs.aws.amazon.com/athena/latest/ug/language-reference.html) tells us that you can't create or drop catalogs and schemas._ 
    On the other hand the [JDBC driver simulates catalog support with a single pseudo-catalog](https://s3.amazonaws.com/athena-downloads/drivers/JDBC/SimbaAthenaJDBC_2.0.7/docs/Simba+Athena+JDBC+Driver+Install+and+Configuration+Guide.pdf#%5B%7B%22num%22%3A40%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C76.5%2C556.31%2C0%5D) called `AwsDataCatalog`.

    And the [documentation of the `SHOW DATABASES` command](https://docs.aws.amazon.com/athena/latest/ug/show-databases.html) states that there is a synonym called `SHOW SCHEMAS`.
    That means that Athena internally creates a 1:1 mapping of databases to schemas with the same name.

1. After we find out about schemas and catalogs, we **implement two unit tests**:

    ```java
    @Test
    void testSupportsJdbcCatalogs() {
        assertThat(this.dialect.supportsJdbcCatalogs(), equalTo(StructureElementSupport.SINGLE));
    }
    
    @Test
    void testSupportsJdbcSchemas() {
        assertThat(this.dialect.supportsJdbcSchemas(), equalTo(StructureElementSupport.MULTIPLE));
    }
    ```

    Both tests must fail. 
    
1. After that **implement the functions `supportsJdbcCatalogs()` and `supportsJdbcSchemas()`**. 
    Don't forget that you have already created the templates for the methods. So you only need to add the returning values.
    Re-run the test to check that your implementation works fine.

1. The methods `requiresCatalogQualifiedTableNames(SqlGenerationContext)` and `requiresSchemaQualifiedTableNames(SqlGenerationContext)` are closely related. 
    They define under which circumstances table names need to be qualified with catalog and / or schema name.
    **Write tests for these two methods and implement them**.

    Below you find two unit test examples where the first checks that the Athena adapter does not require catalog-qualified IDs when generating SQL code and the second states that schema-qualification is required.

    ```java
    @Test
    void testRequiresCatalogQualifiedTableNames() {
        assertThat(this.dialect.requiresCatalogQualifiedTableNames(null), equalTo(false));
    }
    
    @Test
    void testRequiresSchemaQualifiedTableNames() {
        assertThat(this.dialect.requiresSchemaQualifiedTableNames(null), equalTo(true));
    }
    ```

    ### Defining how NULL Values are Sorted
    
    Next we tell the virtual schema how the SQL dialect sorts `NULL` values by default. 
    
    The Athena documentation states that by default `NULL` values appear last in a search result regardless of search direction.

1. So **the unit test** looks like this:

    ```java
    @Test
    void testGetDefaultNullSorting() {
        assertThat(this.dialect.getDefaultNullSorting(), equalTo(NullSorting.NULLS_SORTED_AT_END));
    }
    ```
    
    Again run the test, let it fail, implement, let the test succeed.

    ### Implement String Literal Conversion

    Another thing we need to implement in the dialect class is quoting of string literals.
    
    Athena expects string literals to be wrapped in single quotes and single quotes inside the literal to be escaped by duplicating each.

1. **Create the `testGetLiteralString()` test** method: 
   
    ```java
    @ValueSource(strings = { "ab:'ab'", "a'b:'a''b'", "a''b:'a''''b'", "'ab':'''ab'''" })
    @ParameterizedTest
    void testGetLiteralString(final String definition) {
        assertThat(this.dialect.getStringLiteral(definition.substring(0, definition.indexOf(':'))),
                equalTo(definition.substring(definition.indexOf(':') + 1)));
    }
    ```
    
    You might be wondering why I did not use the `CsvSource` parameterization here.
    This is owed to the fact that the `CsvSource` syntax interprets single quotes as string quotes which makes this particular scenario untestable.

    After we let the test fail, we add the following implementation in the dialect:

    ```java
    @Override
    public String getStringLiteral(final String value) {
         if (value == null) {
             return "NULL";
         } else {
             return "'" + value.replace("'", "''") + "'";
         }
    }
    ```

    ### Implement the Applying of Quotes
    
1. The next method to **implement: `applyQuote()`**. It applies quotes to table and schema names.
    In case of Athena there are two different ways to apply the quotes: "" and ``.
    If an identifier starts with an underscore, we use the backticks. Otherwise, we use double quotes.  
   
    ```java
    @CsvSource({ "tableName, \"tableName\"", //
            "table123, \"table123\"", //
            "_table, `_table`", //
            "123table, \"123table\"", //
            "table_name, \"table_name\"" })
    @ParameterizedTest
    void testApplyQuote(final String unquoted, final String quoted) {
        assertThat(this.dialect.applyQuote(unquoted), equalTo(quoted));
    }
    ```
    And implementation:

    ```java
    @Override
    public String applyQuote(final String identifier) {
       if (this.id.startsWith("_")) {
            return quoteWithBackticks(this.id);
        } else {
            return quoteWithDoubleQuotes(this.id);
        }
    }

    private String quoteWithBackticks(final String identifier) {
        return "`" + identifier + "`";
    }

    private String quoteWithDoubleQuotes(final String identifier) {
        return "\"" + identifier + "\"";
    }   
    ```
    
1. You have **two unimplemented methods** left: `createQueryRewriter()` and `createRemoteMetadataReader()`.
    Let's use a default implementation for now:
    
    ```java
    @Override
    protected RemoteMetadataReader createRemoteMetadataReader() {
        try {
            return new AthenaMetadataReader(this.connectionFactory.getConnection(), this.properties);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                    "Unable to create Athena remote metadata reader. Caused by: " + exception.getMessage(), exception);
        }
    }

    @Override
    protected QueryRewriter createQueryRewriter() {
        return new BaseQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
    }
    ```
    
    Write the tests fot the methods.

    ### Checking the Code Coverage of the Dialect Adapter

    Before you move on, first check how well your unit tests cover the class. 
    Keep adding test until you reach full coverage.

## Creating the SQL Dialect Factory

1. Now **create a class for the factory**: `com.exasol.adapter.dialects.athena.AthenaSqlDialectFactory` that **extends** `AbstractSqlDialectFactory`. 
    Let your IDE to generate necessary **overriding methods** for you. 
  
    ```java
    public class AthenaSqlDialectFactory extends AbstractSqlDialectFactory {
       //methods here
   } 
  
   ```
   
   Also create a corresponding test class.
   
2. Implement method the following methods and write tests.
  
    ```java
    @Override
    protected String getSqlDialectName() {
        return AthenaSqlDialect.NAME;
    }
   
   @Override
    protected String getSqlDialectVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/athena-virtual-schema/pom.properties");
        return versionCollector.getVersionNumber();
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new AthenaSqlDialect(connectionFactory, properties);
    }
    ```
   
    Note that at this point we don't have an instance of the dialect yet and thus are using the constant directly.

## Making the SQL Dialect Factory available for loading

1. **Add the fully qualified factory name** previously implemented, `com.exasol.adapter.dialects.athena.AthenaSqlDialectFactory`, to the **`/src/main/resources/META-INF/services/com.exasol.adapter.dialects.SqlDialectFactory`** file so that the class loader can find the SQL dialect factory.
