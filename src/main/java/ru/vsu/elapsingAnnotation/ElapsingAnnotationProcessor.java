package ru.vsu.elapsingAnnotation;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SupportedAnnotationTypes(value = {"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ElapsingAnnotationProcessor extends AbstractProcessor {

    private static final String ANNOTATION_TYPE = "ru.vsu.elapsingAnnotation.Elapsing";
    private JavacProcessingEnvironment javacProcessingEnv;
    private TreeMaker maker;
    private Messager messager;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        messager = procEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.WARNING, "BLA");
        //  messager.printMessage(Diagnostic.Kind.ERROR, "ERRORInit");
        //  messager.printMessage(Diagnostic.Kind.ERROR, "ERRORInit2");
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.WARNING, "f2");

        // messager.printMessage(Diagnostic.Kind.ERROR, "ERRORproc");

        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

//        JavaFileObject builderFile = null;
//        try {
//            builderFile = processingEnv.getFiler()
//                    .createSourceFile("Class1");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
//
//            out.println("package ru.vsu.annotations;");
//            out.println("public class Class1 {");
//            out.println("}");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        final Elements elements = javacProcessingEnv.getElementUtils();

        final TypeElement annotation = elements.getTypeElement(ANNOTATION_TYPE);

        if (annotation != null) {
            // Выбираем все элементы, у которых стоит наша аннотация
            final Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(annotation);

            JavacElements utils = javacProcessingEnv.getElementUtils();
            for (final Element m : methods) {
                Elapsing elapsing = m.getAnnotation(Elapsing.class);

                if (elapsing != null) {
                    JCTree blockNode = utils.getTree(m);
                    // Нам нужны только описания методов
                    if (blockNode instanceof JCMethodDecl) {
                        // Получаем содержимое метода
                        final List<JCStatement> statements = ((JCMethodDecl) blockNode).body.stats;

                        // Новое тело метода
                        List<JCStatement> newStatements = List.nil();
                        // Добавляем в начало метода сохранение текущего времени
                        JCVariableDecl var = makeTimeStartVar(maker, utils, elapsing);
                        newStatements = newStatements.append(var);


//--------------------------------------------------------------------------------------------------------
                        // Создаём тело блока try, копируем в него оригинальное содержимое метода
                        List<JCStatement> tryBlock = List.nil();
                        for (JCStatement statement : statements) {
                            tryBlock = tryBlock.append(statement);
                        }

                        // Создаём тело блока finally, добавляем в него вывод затраченного времени
                        JCBlock finalizer = makePrintBlock(maker, utils, elapsing, var, m);
                        JCStatement stat = maker.Try(maker.Block(0, tryBlock), List.<JCCatch>nil(), finalizer);
                        newStatements = newStatements.append(stat);
//-----------------------------------------------------------------------------------------------------------

                        // Заменяем старый код метода на новый
                        ((JCMethodDecl) blockNode).body.stats = newStatements;
                    }
                }
            }

            return true;
        }

        return false;
    }

    private JCExpression makeCurrentTime(TreeMaker maker, JavacElements utils, Elapsing elapsing) {
        // Создаём вызов System.nanoTime или System.currentTimeMillis
        JCExpression exp = maker.Ident(utils.getName("System"));
        String methodName;
        switch (elapsing.interval()) {
            case NANOSECOND:
                methodName = "nanoTime";
                break;
            default:
                methodName = "currentTimeMillis";
                break;
        }
        exp = maker.Select(exp, utils.getName(methodName));
        return maker.Apply(List.<JCExpression>nil(), exp, List.<JCExpression>nil());
    }

    protected JCVariableDecl makeTimeStartVar(TreeMaker maker, JavacElements utils, Elapsing elapsing) {
        // Создаём финальную переменную для хранения времени старта. Имя переменной в виде time_start_{random}
        JCExpression currentTime = makeCurrentTime(maker, utils, elapsing);
        String fieldName = "time_start_" + (int) (Math.random() * 10000);
        return maker.VarDef(maker.Modifiers(Flags.FINAL), utils.getName(fieldName), maker.TypeIdent(TypeTag.LONG), currentTime);
    }

    protected JCBlock makePrintBlock(TreeMaker maker, JavacElements utils, Elapsing elapsing, JCVariableDecl var, Element m) {
//        //Logger.getGlobal().log(Level.INFO, String.format(FORMAT, ELAPSED_TIME));
//
//        //Logger.getGlobal().log
        JCExpression getLoggerExpression = maker.Ident(utils.getName("Logger"));
        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("getGlobal"));
        JCExpression getLogger =
                maker.Apply(List.<JCExpression>nil(), getLoggerExpression, List.<JCExpression>nil());
        getLogger=maker.Select(getLogger, utils.getName("log"));

        //Level.INFO
        JCExpression levelExpression=maker.Ident(utils.getName("Level"));
        levelExpression=maker.Select(levelExpression, utils.getName("INFO"));


        //String.format(FORMAT, ELAPSED_TIME)
        JCExpression currentTime = makeCurrentTime(maker, utils, elapsing);
        JCExpression elapsedTime = maker.Binary(Tag.MINUS, currentTime, maker.Ident(var.name));

        JCExpression formatExpression = maker.Ident(utils.getName("String"));
        formatExpression = maker.Select(formatExpression, utils.getName("format"));

        List<JCExpression> formatArgs = List.nil();
        formatArgs = formatArgs.append(maker.Literal(elapsing.format()));
        formatArgs = formatArgs.append(elapsedTime);

        JCExpression format = maker.Apply(List.<JCExpression>nil(), formatExpression, formatArgs);

        List<JCExpression> logArgs = List.nil();
        logArgs = logArgs.append(levelExpression);
        logArgs=logArgs.append(format);


        JCExpression logExpression = maker.Apply(List.<JCExpression>nil(), getLogger, logArgs);
        JCExpressionStatement statement = maker.Exec(logExpression);

        List<JCStatement> statements = List.nil();
        statements = statements.append(statement);

        return maker.Block(0, statements);

//        // Создаём вызов System.out.println
//        JCExpression printlnExpression = maker.Ident(utils.getName("System"));
//        printlnExpression = maker.Select(printlnExpression, utils.getName("out"));
//        printlnExpression = maker.Select(printlnExpression, utils.getName("println"));
//
//        // Создаём блок вычисления затраченного времени (currentTime - startTime)
//        JCExpression currentTime = makeCurrentTime(maker, utils, elapsing);
//        JCExpression elapsedTime = maker.Binary(Tag.MINUS, currentTime, maker.Ident(var.name));
//
//        // Форматируем результат
//        JCExpression formatExpression = maker.Ident(utils.getName("String"));
//        formatExpression = maker.Select(formatExpression, utils.getName("format"));
//
//        // Собираем все кусочки вместе
//        List<JCExpression> formatArgs = List.nil();
//        formatArgs=formatArgs.append(maker.Literal(elapsing.format()));
//        formatArgs=formatArgs.append(elapsedTime);
//
//        JCExpression format = maker.Apply(List.<JCExpression>nil(), formatExpression, formatArgs);
//
//        List<JCExpression> printlnArgs = List.nil();
//        printlnArgs=printlnArgs.append(format);
//
//        JCExpression print = maker.Apply(List.<JCExpression>nil(), printlnExpression, printlnArgs);
//        JCExpressionStatement stmt = maker.Exec(print);
//
//        List<JCStatement> stmts = List.nil();
//        stmts= stmts.append(stmt);
//
//        return maker.Block(0, stmts);
    }

//    @Override
//    public void init(ProcessingEnvironment procEnv) {
//        System.out.println("in init");
//                super.init(procEnv);
//                messager=procEnv.getMessager();
//procEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
////procEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "QWERTY!!!!!!");
////try {
////           // Thread.sleep(5000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
//        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
//    }
//
//    @Override
//    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        messager.printMessage(Diagnostic.Kind.NOTE, "this is a note");
//        System.out.println("in proc");
//        File f=new File("ye");
//        try (PrintWriter printWriter = new PrintWriter(f)) {
//            printWriter.write("qwerty");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }


//        for (Element e : roundEnv.getRootElements()) {
//            System.out.println("Element is " + e.getSimpleName());
//            // Напишите здесь код для анализа каждого корневого элемента
//        }
//
//        if (annotations == null || annotations.isEmpty()) {
//            return false;
//        }
//
//        final Elements elements = javacProcessingEnv.getElementUtils();
//
//        final TypeElement annotation = elements.getTypeElement(ANNOTATION_TYPE);
//
//        if (annotation != null) {
//            // Выбираем все элементы, у которых стоит наша аннотация
//            final Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(annotation);
//
//            JavacElements utils = javacProcessingEnv.getElementUtils();
//            for (final Element messager : methods) {
//                Elapsing elapsing = messager.getAnnotation(Elapsing.class);
//                if (elapsing != null) {
//                    JCTree blockNode = utils.getTree(messager);
//                    // Нам нужны только описания методов
//                    if (blockNode instanceof JCMethodDecl) {
//                        // Получаем содержимое метода
//                        final List<JCStatement> statements = ((JCMethodDecl) blockNode).body.stats;
//
//                        // Новое тело метода
//                        List<JCStatement> newStatements = List.nil();
//                        // Добавляем в начало метода сохранение текущего времени
//                        JCVariableDecl var = makeTimeStartVar(maker, utils, elapsing);
//                        newStatements = newStatements.append(var);
//
//                        // Создаём тело блока try, копируем в него оригинальное содержимое метода
//                        List<JCStatement> tryBlock = List.nil();
//                        for (JCStatement statement : statements) {
//                            tryBlock = tryBlock.append(statement);
//                        }
//
//                        // Создаём тело блока finally, добавляем в него вывод затраченного времени
//                        JCBlock finalizer = makePrintBlock(maker, utils, elapsing, var);
//                        JCStatement stat = maker.Try(maker.Block(0, tryBlock), List.<JCCatch>nil(), finalizer);
//                        newStatements = newStatements.append(stat);
//
//                        // Заменяем старый код метода на новый
//                        ((JCMethodDecl) blockNode).body.stats = newStatements;
//                    }
//                }
//            }
//
//            return true;
//        }

//        return true;
//    }

//    private JCExpression makeCurrentTime(TreeMaker maker, JavacElements utils, Elapsing elapsing) {
//        // Создаём вызов System.nanoTime или System.currentTimeMillis
//        JCExpression exp = maker.Ident(utils.getName("System"));
//        String methodName;
//        switch (elapsing.interval()) {
//            case NANOSECOND:
//                methodName = "nanoTime";
//                break;
//            default:
//                methodName = "currentTimeMillis";
//                break;
//        }
//        exp = maker.Select(exp, utils.getName(methodName));
//        return maker.Apply(List.<JCExpression>nil(), exp, List.<JCExpression>nil());
//    }
//
//    protected JCVariableDecl makeTimeStartVar(TreeMaker maker, JavacElements utils, Elapsing elapsing) {
//        // Создаём финальную переменную для хранения времени старта. Имя переменной в виде time_start_{random}
//        JCExpression currentTime = makeCurrentTime(maker, utils, elapsing);
//        String fieldName = fieldName = "time_start_" + (int) (Math.random() * 10000);
//        return maker.VarDef(maker.Modifiers(Flags.FINAL), utils.getName(fieldName), maker.TypeIdent(TypeTag.LONG), currentTime);
//    }
//
//    protected JCBlock makePrintBlock(TreeMaker maker, JavacElements utils, Elapsing elapsing, JCVariableDecl var) {
//        // Создаём вызов System.out.println
//        JCExpression printlnExpression = maker.Ident(utils.getName("System"));
//        printlnExpression = maker.Select(printlnExpression, utils.getName("out"));
//        printlnExpression = maker.Select(printlnExpression, utils.getName("println"));
//
//        // Создаём блок вычисления затраченного времени (currentTime - startTime)
//        JCExpression currentTime = makeCurrentTime(maker, utils, elapsing);
//        JCExpression elapsedTime = maker.Binary(Tag.MINUS, currentTime, maker.Ident(var.name));
//
//        // Форматируем результат
//        JCExpression formatExpression = maker.Ident(utils.getName("String"));
//        formatExpression = maker.Select(formatExpression, utils.getName("format"));
//
//        // Собираем все кусочки вместе
//        List<JCExpression> formatArgs = List.nil();
//        formatArgs.append(maker.Literal(elapsing.format()));
//        formatArgs.append(elapsedTime);
//
//        JCExpression format = maker.Apply(List.<JCExpression>nil(), formatExpression, formatArgs);
//
//        List<JCExpression> printlnArgs = List.nil();
//        printlnArgs.append(format);
//
//        JCExpression print = maker.Apply(List.<JCExpression>nil(), printlnExpression, printlnArgs);
//        JCExpressionStatement stmt = maker.Exec(print);
//
//        List<JCStatement> stmts = List.nil();
//        stmts.append(stmt);
//
//        return maker.Block(0, stmts);
//    }
}
