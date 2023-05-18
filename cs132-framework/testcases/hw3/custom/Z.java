class Z {
    public static void main(String[] args) {
        Person p;
        int u;
        p = new Student();
        u = p.foo();
    }
}

class Mammal {
    int name;

    public int foo() {
        int u;
        name = 1;
        System.out.println(name);
        u = this.bar();
        return 0;
    }

    public int bar() {
        return 0;
    }
}

class Person extends Mammal {
    int name;

    public int baz() {
        name = 3;
        System.out.println(name);
        return 0;
    }
}

class Student extends Person {
    int name;

    public int bar() {
        int u;
        name = 2;
        System.out.println(name);
        u = this.baz();
        return 0;
    }
}
