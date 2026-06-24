module com.crystalbreak {
    requires com.fasterxml.jackson.databind;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;

    exports com.crystalbreak.app;
    exports com.crystalbreak.ai;
    exports com.crystalbreak.audio;
    exports com.crystalbreak.controller;
    exports com.crystalbreak.ext;
    exports com.crystalbreak.model;
    exports com.crystalbreak.modes;
    exports com.crystalbreak.persistence;
    exports com.crystalbreak.physics;
    exports com.crystalbreak.util;
    exports com.crystalbreak.view;

    opens com.crystalbreak.persistence to com.fasterxml.jackson.databind;
}
