package ru.vsu.elapsingAnnotation;

import com.google.auto.service.AutoService;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;

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
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
        this.messager = procEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        JavacElements utils = javacProcessingEnv.getElementUtils();
        final Elements elements = javacProcessingEnv.getElementUtils();
        final TypeElement annotation = elements.getTypeElement(ANNOTATION_TYPE);

        if (annotation != null) {
            // Выбираем все элементы, у которых стоит наша аннотация


//            ImportScanner scanner = new ImportScanner();
//            scanner.scan(roundEnv.getRootElements(), null);
//
//            Set<String> importedTypes = scanner.getImportedTypes();
//            importedTypes.forEach(s ->
//                    messager.printMessage(Diagnostic.Kind.ERROR, s));

//
//
//            final Set<? extends Element> classes = roundEnv.getRootElements();
//
//
//            classes.forEach(clazz ->
//                    clazz.getEnclosedElements().forEach(method -> {
//                        if (method.getAnnotation(Elapsing.class) != null){
//                            JCTree body =  utils.getTree(clazz);
//                            List<JCStatement> stats = List.nil();
//                            stats.addAll(((JCMethodDecl) body).body.stats);
//
//                            TypeElement list =
//                                    workingCopy.getElements().getTypeElement("java.util.Map.Entry");
//                            Types types = workingCopy.getTypes();
//                            TypeMirror tm = types.getArrayType(types.erasure(list.asType()));
//                            stats.add(maker.Variable(maker.Modifiers(Collections.<Modifier>emptySet()), "entry", maker.Type(tm), null));
//
//                            ((JCMethodDecl) body).body.stats = stats;
//                        }
//
//                            //messager.printMessage(Diagnostic.Kind.ERROR, method.getSimpleName());
//                    }));
//


            final Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(annotation);
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
                        JCVariableDecl timeStartVarDecl =
                                makeTimeVar(maker, utils, "start", maker.Modifiers(Flags.FINAL),
                                        makeCurrentTime(maker, utils, elapsing));
                        newStatements = newStatements.append(timeStartVarDecl);

                        JCVariableDecl timeFinishVarDecl = makeTimeVar(maker, utils, "finish", maker.Modifiers(0),
                                maker.Literal(0));
                        newStatements = newStatements.append(timeFinishVarDecl);

                        // Создаём тело блока try, копируем в него оригинальное содержимое метода
                        List<JCStatement> tryBlock = List.nil();

                        for (JCStatement statement : statements) {
                            tryBlock = tryBlock.append(statement);
                        }

                        tryBlock = tryBlock.append(maker.Exec(maker.Assign(maker.Ident(utils.getName(timeFinishVarDecl.name)), makeCurrentTime(maker, utils, elapsing))));

                        // Создаём тело блока finally, добавляем в него вывод затраченного времени
                        JCBlock finalizer = makePrintBlock(maker, utils, elapsing, timeStartVarDecl, timeFinishVarDecl);
                        JCStatement stat = maker.Try(maker.Block(0, tryBlock), List.<JCCatch>nil(), finalizer);
                        newStatements = newStatements.append(stat);

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

    protected JCVariableDecl makeTimeVar(TreeMaker maker, JavacElements utils,
                                         String name, JCModifiers modifiers, JCExpression value) {
        String fieldName = "time_" + name + "_" + System.currentTimeMillis();
        return maker.VarDef(modifiers, utils.getName(fieldName), maker.TypeIdent(TypeTag.LONG), value);
    }


    private JCExpressionStatement logElapsed(TreeMaker maker, JavacElements utils, String level, JCExpression loggedText) {
        //Logger.getGlobal().log(Level.INFO, String.format(FORMAT, ELAPSED_TIME));
        //Logger.getGlobal().log(level, loggedText);
        //Logger.getGlobal().log
        JCExpression getLoggerExpression = maker.Ident(utils.getName("Logger"));
        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("getGlobal"));
        JCExpression getLogger =
                maker.Apply(List.<JCExpression>nil(), getLoggerExpression, List.<JCExpression>nil());
        getLogger = maker.Select(getLogger, utils.getName("log"));

        //Level.INFO
        JCExpression levelExpression = maker.Ident(utils.getName("Level"));
        levelExpression = maker.Select(levelExpression, utils.getName(level));

        List<JCExpression> logArgs = List.nil();
        logArgs = logArgs.append(levelExpression);
        logArgs = logArgs.append(loggedText);


        JCExpression logExpression = maker.Apply(List.<JCExpression>nil(), getLogger, logArgs);
        return maker.Exec(logExpression);
    }

    protected JCBlock makePrintBlock(TreeMaker maker, JavacElements utils, Elapsing elapsing, JCVariableDecl var, JCVariableDecl varF) {

        List<JCStatement> statements = List.nil();

        //log elapsed time

        // String.format(FORMAT, ELAPSED_TIME)
        JCExpression elapsedTime = maker.Binary(Tag.MINUS, maker.Ident(utils.getName(varF.name)), maker.Ident(var.name));

        JCExpression formatExpression = maker.Ident(utils.getName("String"));
        formatExpression = maker.Select(formatExpression, utils.getName("format"));

        List<JCExpression> formatArgs = List.nil();
        formatArgs = formatArgs.append(maker.Literal(elapsing.format()));
        formatArgs = formatArgs.append(elapsedTime);

        JCExpression format = maker.Apply(List.nil(), formatExpression, formatArgs);

        JCExpressionStatement logElapsedTime = logElapsed(maker, utils, Level.INFO.getName().toUpperCase(), format);
        statements = statements.append(logElapsedTime);

        //log delta elapsed if method elapsed more than said
        JCExpression condition = maker.Binary(
                Tag.GT, //greater
                maker.Binary(Tag.MINUS, makeCurrentTime(maker, utils, elapsing), maker.Ident(var.name)),//currTime-startTime=elapsed
                maker.Literal(elapsing.maxElapsed())); //maxElapsed

        JCStatement ifStatement = maker.If(
                condition,
                //then
                logElapsed(maker, utils, Level.WARNING.getName().toUpperCase(),
                        //loggedText
                        maker.Binary(Tag.PLUS,
                                maker.Binary(Tag.PLUS, maker.Literal("Elapsed "),
                                        maker.Binary(Tag.MINUS, elapsedTime, maker.Literal(elapsing.maxElapsed()))),
                                maker.Literal(" " + elapsing.interval().name().toLowerCase() + " more"))),
                /*else*/ null);

        statements = statements.append(ifStatement);
        return maker.Block(0, statements);
    }
}
