@0xecc23a6ce1dbd595;

using Java = import "/java.capnp";
$Java.package("com.codecool.ccsima2.preliminary");
$Java.outerClassname("RequestClass");

using import "Bugfix.capnp".Bugfix;


struct Request {
    union {
        login : group {
            team @0 : Text;
            hash @1 : Text;
        }
        bugfix @2 : Bugfix;
    }
}
