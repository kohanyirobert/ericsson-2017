@0xc037d851bb528efa;

using Java = import "/java.capnp";
$Java.package("com.codecool.ccsima2.preliminary");
$Java.outerClassname("ResponseClass");

using import "Bugfix.capnp".Bugfix;


struct Response {
    status @0 : Text;
    union {
        bugfix @1 : Bugfix;
        end @2 : Bool;
    }
}
