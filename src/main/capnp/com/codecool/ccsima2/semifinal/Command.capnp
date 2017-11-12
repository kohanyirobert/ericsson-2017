@0xabf8f53c554fa923;

using Java = import "/java.capnp";
$Java.package("com.codecool.ccsima2.semifinal");
$Java.outerClassname("CommandClass");

using Common = import "Common.capnp";

struct Move {
    unit @0 : Int32;
    direction @1 : Common.Direction;
}

struct Command {
    commands : union {
        moves @0 : List(Move);
        login : group {
            team @1 : Text;
            hash @2 : Text;
        }
    }
}
