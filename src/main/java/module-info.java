module com.example.hidocproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.formsfx;
    requires java.sql;
    requires javafx.media;
    requires itextpdf;
    requires org.apache.poi.ooxml;
    requires java.desktop;

    exports com.example.cobaproject03;
    exports Connection;
    exports Interface;
    exports Admin;
    exports Admin.Main;
    exports Admin.Controller;
    exports Apoteker;
    exports Apoteker.Main;
    exports Apoteker.Controller;
    exports Manajer.Controller;
//    exports Manajer.Main;

    opens Interface to javafx.fxml;
    opens Admin to javafx.fxml;
    opens Admin.Main to javafx.fxml;
    opens Admin.Controller to javafx.fxml;
    opens Apoteker to javafx.fxml;
    opens Apoteker.Main to javafx.fxml;
    opens Apoteker.Controller to javafx.fxml;
    opens Manajer to javafx.fxml;
    opens Manajer.Controller to javafx.fxml;
//    opens Manajer.Controller to javafx.fxml;
}