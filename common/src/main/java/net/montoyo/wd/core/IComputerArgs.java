package net.montoyo.wd.core;

public interface IComputerArgs {
    int count();
    int checkInteger(int index);
    double checkDouble(int index);
    String checkString(int index);
    <T> T checkTable(int index);
}
