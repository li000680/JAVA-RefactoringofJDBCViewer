package lab1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * 
 * 
 * @author Wei Li
 * @version July 21, 2020
 */
public class JDBCViewerSolution extends Application{

	/**
	 * width of the scene
	 */
	private static final double WIDTH = 600;
	/**
	 * height of the scene
	 */
	private static final double HEIGHT = 400;
	/**
	 * title of the application
	 */
	private static final String TITLE = "JDBC Viewer";
	/**
	 * URL path to database
	 */
	private static final String DB_URL = "jdbc:mysql://localhost:3306/redditreader";
	/**
	 * SQL search script for getting all
	 */
	private static final String SQL_SCRIPT_SELECT_ALL = "SELECT * FROM redditreader.account";
	/**
	 * SQL search script for getting all with condition
	 */
	private static final String SQL_SCRIPT_SELECT_WHERE = "SELECT * FROM redditreader.account where nickname like ? or username like ?";
	/**
	 * names of the columns
	 */
	private static final String[] COLUMN_NAMES = { "ID", "Nickname", "Username", " Password" };
	/**
	 * username used in the DB
	 */
	private static final String USERNAME = "cst8288";
	/**
	 * password used in the DB
	 */
	private static final String PASSWORD = "8288";

	/**
	 * {@link BorderPane} is a layout manager that manages all nodes in 5 areas as below:
	 * 
	 * <pre>
	 * -----------------------
	 * |        top          |
	 * -----------------------
	 * |    |          |     |
	 * |left|  center  |right|
	 * |    |          |     |
	 * -----------------------
	 * |       bottom        |
	 * -----------------------
	 * </pre>
	 * 
	 * this object is passed to {@link Scene} object in {@link JDBCViewerSolution#start(Stage)} method.
	 */
	private BorderPane root;
	private Connection connection;
	private Label conectionStatus;
	private WebView webView;

	/**
	 * this method is called at the very beginning of the JavaFX application and can be used to initialize
	 * all components in the application. however, {@link Scene} and {@link Stage} must not be created in
	 * this method. this method does not run JavaFX thread, it runs on JavaFX-Launcher thread.
	 */
	@Override
	public void init() throws Exception{

		Region optionsBar = createOptionsBar();
		Region statusBar = createStatusBar();
		
		root = new BorderPane();
		root.setTop( optionsBar);
		root.setBottom( statusBar);
	}

	/**
	 * <p>
	 * this method is called when JavaFX application is started and it is running on JavaFX thread.
	 * this method must at least create {@link Scene} and finish customizing {@link Stage}. these two
	 * objects must be on JavaFX thread when created.
	 * </p>
	 * <p>
	 * {@link Stage} represents the frame of your application, such as minimize, maximize and close buttons.<br>
	 * {@link Scene} represents the holder of all your JavaFX {@link Node}s.<br>
	 * {@link Node} is the super class of every javaFX class.
	 * </p>
	 * @param primaryStage - primary stage of your application that will be rendered
	 */
	@Override
	public void start( Stage primaryStage) throws Exception{
		// scene holds all JavaFX components that need to be displayed in Stage
		Scene scene = new Scene( root, WIDTH, HEIGHT);
		primaryStage.setScene( scene);
		primaryStage.setTitle( TITLE);
		primaryStage.setResizable( true);
		// when escape key is pressed close the application
		primaryStage.addEventHandler( KeyEvent.KEY_RELEASED, ( KeyEvent event) -> {
			if( KeyCode.ESCAPE == event.getCode()){
				primaryStage.hide();
			}
		});
		webView = new WebView();
		root.setCenter( webView);
		// display the JavaFX application
		primaryStage.show();
	}

	/**
	 * this method is called at the very end when the application is about to exit.
	 * this method is used to stop or release any resources used during the application.
	 */
	@Override
	public void stop() throws Exception{
		if( connection != null)
			connection.close();
	}

	private Button createButton( String name, EventHandler< ActionEvent> onClick){
		Button button = new Button( name);
		button.setMaxSize( Double.MAX_VALUE, Double.MAX_VALUE);
		button.setOnAction( onClick);
		return button;
	}

	private TextField createTextField( String value, String prompt){
		TextField textField = new TextField( value);
		textField.setPromptText( prompt);
		GridPane.setHgrow( textField, Priority.ALWAYS);
		return textField;
	}

	private PasswordField createPasswordField( String value, String prompt){
		PasswordField passField = new PasswordField();
		passField.setText( value);
		passField.setPromptText( prompt);
		GridPane.setHgrow( passField, Priority.ALWAYS);
		return passField;
	}

	/**
	 * create a {@link MenuBar} that represent the menu bar at the top of the application.
	 * @return customized {@link MenuBar} as its super class {@link Region}.
	 */
	private Region createOptionsBar(){

		TextField dbURLText = createTextField( DB_URL, "DB URL");
		TextField userText = createTextField( USERNAME, "Username");
		TextField searchText = createTextField( "", "Search Text");
		PasswordField passText = createPasswordField( PASSWORD, "Password");

		Button connectButton = createButton( "Connect", e -> {
			try{
				conectionStatus.setText( "connecting");
				connectTo( dbURLText.getText(), userText.getText(), passText.getText());
				conectionStatus.setText( "connected");
			}catch( SQLException ex){
				conectionStatus.setText( "failed: " + ex.getMessage());
//				Logger.getLogger( this.getClass().getName()).log( Level.SEVERE, ex.getMessage(), ex);
				ex.printStackTrace();
			}
		});
		Button searchButton = createButton( "Search", e -> {
			try{
				conectionStatus.setText( "searching");
				ObservableList< List< String>> list = search( searchText.getText().trim());
				if( list == null)
					conectionStatus.setText( "must connect first");
				else
					populateTextArea( list);
				conectionStatus.setText( "finished");
			}catch( SQLException ex){
				conectionStatus.setText( "failed: " + ex.getMessage());
				ex.printStackTrace();
			}
		});
		searchText.setOnAction( e -> searchButton.fire());

		GridPane grid = new GridPane();
		grid.setHgap( 3);
		grid.setVgap( 3);
		grid.setPadding( new Insets( 5, 5, 5, 5));

		grid.add( dbURLText, 0, 0, 2, 1);
		grid.add( userText, 0, 1, 1, 1);
		grid.add( passText, 1, 1, 1, 1);
		grid.add( connectButton, 2, 0, 1, 2);
		grid.add( searchText, 0, 2, 2, 1);
		grid.add( searchButton, 2, 2, 1, 1);

		return grid;
	}

	/**
	 * create a {@link ToolBar} that will represent the status bar of the application.
	 * @return customized {@link ToolBar} as its super class {@link Region}.
	 */
	private Region createStatusBar(){
		conectionStatus = new Label( "Not Connected");

		return new ToolBar( conectionStatus);
	}

	private void populateTextArea( ObservableList< List< String>> data){
		StringBuilder builder = new StringBuilder();

		builder.append( "<table style=\"margin: auto;width: 90%;\" border=\"1\">");
		builder.append( "<caption>Account Table</caption>");

		builder.append( "<tr style=\"height: 2rem;\">");
		for( String header : COLUMN_NAMES){
			builder.append( "<th>");
			builder.append( header);
			builder.append( "</th>");
		}
		builder.append( "</tr>");

		for( List< String> row : data){
			builder.append( "<tr>");
			for( String value : row){
				builder.append( "<td>");
				builder.append( value);
				builder.append( "</td>");
			}
			builder.append( "</tr>");
		}

		builder.append( "</table>");
		webView.getEngine().loadContent( builder.toString());
	}

	private void connectTo( String dbURL, String user, String pass) throws SQLException{
		if( connection == null)
			connection = DriverManager.getConnection( dbURL, user, pass);
	}

	/**
	 * Search method. This method is called after the connection to the DB is established.
	 * <ol>
	 * <li>In this method create a list with the same type as return type.</li>
	 * <li>Determine if <code>searchTrem</code> is null or empty, if so use {@link JDBCViewerSolution#SQL_SCRIPT_SELECT_ALL} in prepared statement.
	 * Otherwise use {@link JDBCViewerSolution#SQL_SCRIPT_SELECT_WHERE}.</li>
	 * <li>Create an instance of {@link PreparedStatement}, since it's a closable resource use it inside of a try with resource. To get a
	 * {@link PreparedStatement} use the method {@link Connection#prepareStatement(String)},
	 * the argument for this method will be the SQL script decided in step 2.</li>
	 * <li>Inside of try with resource</li>
	 * <li>Depending on step 2 set parameters based on <code>searchTrem</code>. Don't forget all parameters are the same and first
	 * index starts at 1 not zero. Since the SQL script will use like with wild card add "%" to the beginning and end of
	 * <code>searchTrem</code> before setting the parameters.</li>
	 * <li>Create a new try with resource and initialize {@link ResultSet}. To initialize it use the method {@link PreparedStatement#executeQuery()}
	 * <li>Create <code>while</code> loop with condition {@link ResultSet#next()}. Each <code>next()</code> will move the result
	 * to the next row. <code>next()</code> must always be called first.</li>
	 * <li>For each row create a new loop and count 5 times.</li>
	 * Using the method {@link ResultSet#getObject(int)} grab each column and add it to the list from step 1.</li>
	 * <li>Finally return the populated list created in step 1.</li>
	 * </ol>
	 * @param searchTerm - a nullable string which represent what text to search on DB, empty or null mean get everything.
	 * @return return a two dimensional list data retrieved from DB. First dimension are the rows.
	 *         The type of this list is "?" as we don't know each type.
	 * @throws SQLException this exception is if there is an issue with DB access,
	 *             connection is closed or any exceptions forwarded from {@link Connection#prepareStatement(String)}
	 *             or {@link PreparedStatement#executeQuery()}.
	 */
	private ObservableList< List< String>> search( String searchTerm) throws SQLException{
		if( connection == null || connection.isClosed())
			return null;
		ObservableList< List< String>> list = FXCollections.observableArrayList();
		boolean fullSearch = searchTerm == null || searchTerm.isEmpty();
		String query = fullSearch ? SQL_SCRIPT_SELECT_ALL : SQL_SCRIPT_SELECT_WHERE;
		try( PreparedStatement ps = connection.prepareStatement( query)){
			if( !fullSearch){
				searchTerm = "%" + searchTerm + "%";
				ps.setString( 1, searchTerm);
				ps.setString( 2, searchTerm);
			}
			try( ResultSet rs = ps.executeQuery()){
				while( rs.next()){
					List< String> row = new ArrayList<>( 5);
					for( int i = 1; i <= COLUMN_NAMES.length; i++){
						row.add( rs.getString( i));
					}
					list.add( row);
				}
			}
		}
		return list;
	}

	/**
	 * main starting point of the application
	 * @param args - arguments provided through command line, if any
	 */
	public static void main( String[] args){
		// launch( args); method starts the javaFX application.
		// some IDEs are capable of starting JavaFX without this method.
		launch( args);
	}
}
