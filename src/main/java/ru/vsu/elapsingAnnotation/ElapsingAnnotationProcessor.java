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
import java.util.Set;
import java.util.logging.Level;


/**
 * processes @Elapsing
 *
 * @see AbstractProcessor
 * @see Processor
 */
@SupportedAnnotationTypes(value = "ru.vsu.elapsingAnnotation.Elapsing")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ElapsingAnnotationProcessor extends AbstractProcessor {

    private static final String ANNOTATION_TYPE = "ru.vsu.elapsingAnnotation.Elapsing";
    private JavacProcessingEnvironment javacProcessingEnvironment;
    private TreeMaker treeMaker;
    private JavacElements utils;
    private Messager messager;

    /**
     * initialises variables for annotation processing
     *
     * @param processingEnv processingEnvironment
     */
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javacProcessingEnvironment = (JavacProcessingEnvironment) processingEnv;
        this.treeMaker = TreeMaker.instance(javacProcessingEnvironment.getContext());
        this.messager = processingEnv.getMessager();
        this.utils = javacProcessingEnvironment.getElementUtils();
    }

    /**
     * processes supported annotations
     *
     * @param annotations annotations that will be processed
     * @param roundEnv    roundEnvironment
     * @return whether or not the set of annotation types are claimed by this processor
     * @see SupportedAnnotationTypes
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        final Elements elements = javacProcessingEnvironment.getElementUtils();
        final TypeElement annotation = elements.getTypeElement(ANNOTATION_TYPE);

        if (annotation != null) {
            final Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(annotation);
            for (final Element method : methods) {
                Elapsing elapsingAnnotation = method.getAnnotation(Elapsing.class);

                if (elapsingAnnotation != null) {

                    validAttributes(elapsingAnnotation);

                    JCTree methodDeclNode = utils.getTree(method);
                    if (methodDeclNode instanceof JCMethodDecl) {
                        // get method statements
                        final List<JCStatement> methodStatements = ((JCMethodDecl) methodDeclNode).body.stats;

                        // new method body
                        List<JCStatement> newStatements = List.nil();

                        //add start and finish time to the beginning of the method
                        JCVariableDecl timeStartVarDecl = makeTimeVar("start", treeMaker.Modifiers(Flags.FINAL), makeCurrentTime());
                        newStatements = newStatements.append(timeStartVarDecl);

                        JCVariableDecl timeFinishVarDecl = makeTimeVar("finish", treeMaker.Modifiers(0), makeCurrentTime());
                        newStatements = newStatements.append(timeFinishVarDecl);

                        // create try block
                        List<JCStatement> tryBlock = List.nil();

                        //append try block with method statements
                        for (JCStatement statement : methodStatements) {
                            tryBlock = tryBlock.append(statement);
                        }

                        //save finish time
                        tryBlock = tryBlock.append(treeMaker.Exec(treeMaker.Assign
                                (treeMaker.Ident(utils.getName(timeFinishVarDecl.name)),
                                        makeCurrentTime())));

                        // create finally block, append with time logging
                        JCBlock finallyBlock =
                                makeLogBlock(elapsingAnnotation,
                                        timeStartVarDecl, timeFinishVarDecl,
                                        method.getEnclosingElement().getSimpleName().toString(),
                                        method.getSimpleName().toString());

                        //add try-finally block to newStatements
                        newStatements = newStatements.append(treeMaker.Try
                                (treeMaker.Block(0, tryBlock), List.nil(), finallyBlock));

                        // save replace method body with new statements
                        ((JCMethodDecl) methodDeclNode).body.stats = newStatements;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * validates annotation attributes
     *
     * @param elapsingAnnotation @Elapsing
     */
    private void validAttributes(Elapsing elapsingAnnotation) {
        if (!elapsingAnnotation.messageFormat().equals(Elapsing.DEFAULT_MESSAGE) && !elapsingAnnotation.messageFormat().contains("%s")) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Param messageFormat should contain '%s' otherwise time will not be logged");

        }
        if (!elapsingAnnotation.overtimeMessageFormat().equals(Elapsing.DEFAULT_MESSAGE) && !elapsingAnnotation.overtimeMessageFormat().contains("%s")) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Param overtimeMessageFormat should contain '%s' otherwise time will not be logged");

        }
        if (elapsingAnnotation.maxElapsed() < 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Invalid maxElapsed param: must not be negative");

        }
    }

    /**
     * @return System.nanoTime() or System.currentTimeMillis() expression
     */
    private JCExpression makeCurrentTime() {
        //create System.currentTimeMillis call
        JCExpression timeCallExpression = treeMaker.Ident(utils.getName("System"));
        timeCallExpression = treeMaker.Select(timeCallExpression, utils.getName("currentTimeMillis"));
        return treeMaker.Apply(List.nil(), timeCallExpression, List.nil());
    }


    /**
     * creates time variable declaration and initialisation
     *
     * @param name      time varible name
     * @param modifiers variable modifiers
     * @param value     variable initial value
     * @return time variable declaration, eg final long time_start_1615004237="[value]";
     */
    private JCVariableDecl makeTimeVar(String name, JCModifiers modifiers, JCExpression value) {
        //create new time variable definition
        String fieldName = "time_" + name + "_" + System.currentTimeMillis();
        return treeMaker.VarDef(modifiers, utils.getName(fieldName), treeMaker.TypeIdent(TypeTag.LONG), value);
    }


    /**
     * creates log statement
     *
     * @param level      logging level
     * @param loggedText text that will be logged
     * @return logging expression, eg Loggable.getGlobal().log(level, loggedText));
     */
    private JCExpressionStatement logElapsed(String level, String className, String methodName, JCExpression loggedText) {
        //Loggable.log(level, loggedText);

        //Loggable.log
        JCExpression getLoggerExpression = getElapsingConfigGetter("Loggable");
        JCExpression logCallExpression = treeMaker.Select(getLoggerExpression, utils.getName("log"));

        JCExpression levelExpression = treeMaker.Ident(utils.getName("java"));
        levelExpression = treeMaker.Select(levelExpression, utils.getName("util"));
        levelExpression = treeMaker.Select(levelExpression, utils.getName("logging"));
        levelExpression = treeMaker.Select(levelExpression, utils.getName("Level"));
        levelExpression = treeMaker.Select(levelExpression, utils.getName(level));

        List<JCExpression> logArgs = List.nil();
        logArgs = logArgs.append(levelExpression);
        logArgs = logArgs.append(treeMaker.Literal(className));
        logArgs = logArgs.append(treeMaker.Literal(methodName));
        logArgs = logArgs.append(loggedText);

        JCExpression logExpression = treeMaker.Apply(List.nil(), logCallExpression, logArgs);
        return treeMaker.Exec(logExpression);
    }

    /**
     * creates block of statements tht logs time
     *
     * @param timeStartVar  start time variable declaration
     * @param timeFinishVar finish time variable declaration
     * @return block of statements tht logs time
     */
    private JCBlock makeLogBlock(Elapsing elapsing, JCVariableDecl timeStartVar, JCVariableDecl timeFinishVar, String className, String methodName) {
        List<JCStatement> statements = List.nil();

        //log elapsed time

        // String.format(FORMAT, ELAPSED_TIME)
        JCExpression elapsedTime = treeMaker.Binary(
                Tag.MINUS,
                treeMaker.Ident(utils.getName(timeFinishVar.name)),
                treeMaker.Ident(utils.getName(timeStartVar.name)));


        JCExpression formatExpression = treeMaker.Ident(utils.getName("String"));
        formatExpression = treeMaker.Select(formatExpression, utils.getName("format"));

        List<JCExpression> formatArgs = List.nil();

        if (!elapsing.messageFormat().equals(Elapsing.DEFAULT_MESSAGE)) {
            formatArgs = formatArgs.append(treeMaker.Literal(elapsing.messageFormat())); //format
        } else {
            formatArgs = formatArgs.append(getElapsingConfigGetter("MessageFormat"));
        }
        formatArgs = formatArgs.append(elapsedTime); //ELAPSED_TIME
        JCExpression format = treeMaker.Apply(List.nil(), formatExpression, formatArgs);


        JCExpressionStatement logElapsedTime = logElapsed(Level.INFO.getName().toUpperCase(), className, methodName, format);
        statements = statements.append(logElapsedTime);
        //----------------------------------------------------------------------------------------------------------------------------//

        //log delta elapsed if method elapsed more than said

        JCExpression maxElapsedExpression;
        if (elapsing.maxElapsed() != 0) {
            maxElapsedExpression = treeMaker.Literal(elapsing.maxElapsed()); //format
        } else {
            maxElapsedExpression = getElapsingConfigGetter("MaxElapsed");
        }

        JCExpression overtimeMessageFormatExpression;
        if (!elapsing.overtimeMessageFormat().equals(Elapsing.DEFAULT_MESSAGE)) {
            overtimeMessageFormatExpression = treeMaker.Literal(elapsing.overtimeMessageFormat()); //format
        } else {
            overtimeMessageFormatExpression = getElapsingConfigGetter("OvertimeMessageFormat");
        }

        JCExpression condition = treeMaker.Binary(
                Tag.GT, //greater
                treeMaker.Binary(Tag.MINUS, makeCurrentTime(), treeMaker.Ident(timeStartVar.name)),//currTime-startTime=elapsed
                maxElapsedExpression); //maxElapsed

        formatArgs = List.nil();
        formatArgs = formatArgs.append(overtimeMessageFormatExpression); //format
        formatArgs = formatArgs.append(treeMaker.Binary(Tag.MINUS, elapsedTime, maxElapsedExpression)); //ELAPSED_TIME-maxElapsed
        format = treeMaker.Apply(List.nil(), formatExpression, formatArgs);
        JCStatement ifStatement = treeMaker.If(
                condition,
                //then
                logElapsed(Level.WARNING.getName().toUpperCase(), className, methodName, format),
                /*else*/ null);

        statements = statements.append(ifStatement);
        return treeMaker.Block(0, statements);
    }

    private JCExpression getElapsingConfigGetter(String getterName) {
        JCExpression expression = treeMaker.Ident(utils.getName("ru"));
        expression = treeMaker.Select(expression, utils.getName("vsu"));
        expression = treeMaker.Select(expression, utils.getName("elapsingAnnotation"));
        expression = treeMaker.Select(expression, utils.getName("ElapsingConfig"));
        expression = treeMaker.Select(expression, utils.getName("getInstance"));
        expression = treeMaker.Apply(List.nil(), expression, List.nil());
        expression = treeMaker.Select(expression, utils.getName("get" + getterName));
        return treeMaker.Apply(List.nil(), expression, List.nil());
    }


}
