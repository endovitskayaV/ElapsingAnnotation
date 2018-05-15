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
    private Messager messager;
    private ElapsingConfig elapsingConfig;

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


        JavacElements utils = javacProcessingEnvironment.getElementUtils();
        final Elements elements = javacProcessingEnvironment.getElementUtils();
        final TypeElement annotation = elements.getTypeElement(ANNOTATION_TYPE);

        if (annotation != null) {

//---------------------------------------------------------
//            ImportScanner scanner = new ImportScanner();
//            scanner.scan(roundEnv.getRootElements(), null);
//            Set<String> importedTypes = scanner.getImportedTypes();
//            importedTypes.forEach(s ->
//                    messager.printMessage(Diagnostic.Kind.ERROR, s));
//
//            messager.printMessage(Diagnostic.Kind.ERROR,"fsdf");
//            final Set<? extends Element> classes = roundEnv.getRootElements();
//            classes.forEach(o -> o.getEnclosedElements().forEach(o1 ->
//                    messager.printMessage(Diagnostic.Kind.ERROR,o.toString())));

//
//            classes.forEach(clazz ->
//                    clazz.getEnclosedElements().forEach(method -> {
//                        if (method.getAnnotation(Elapsing.class) != null) {
//                            JCTree classDeclNode = utils.getTree(clazz);
//                            final List<JCTypeParameter> classStatements = ((JCClassDecl)classDeclNode).typarams;
//                            classStatements.append(new JCTypeParameter("", null, null));
//                            ((JCClassDecl) classDeclNode).typarams = classStatements;
//
//                            // new method body
//                            List<JCStatement> newStatements = List.nil();
//
//                            treeMaker.Import(treeMaker.Ident(utils.getName("")), false)

//                            JCTree clazzDeclNode = utils.getTree(clazz);
//                            JCClassDecl clDecl = ((JCClassDecl) clazzDeclNode);//.body.stats;
//                           clDecl.defs.forEach(jcExpression ->messager.printMessage(Diagnostic.Kind.ERROR,"defs"+jcExpression.toString()));
//                           messager.printMessage(Diagnostic.Kind.ERROR,"mods"+ clDecl.mods.toString());
//                            messager.printMessage(Diagnostic.Kind.ERROR,"type"+clDecl.type.toString());


// clDecl.
//                                     .forEach(jcTree -> messager.printMessage(Diagnostic.Kind.ERROR, jcTree.type.stringValue()));
//                        }
//                    }));

//                            JCNewClass jcNewClass=new JCNewClass();
//                            final List<JCStatement> methodStatements = ((JCClassDecl) clazzDeclNode).body.stats;
//                            ((JCClassDecl) clazzDeclNode).
//                            JCClassDecl classDecl=new JCClassDecl();
//                            classDecl.
//                            JCTree body =  utils.getTree(clazz);
//                            List<JCStatement> stats = List.nil();
//                            stats.addAll(((JCMethodDecl) body).body.stats);
//
//                            TypeElement list =
//                                    workingCopy.getElements().getTypeElement("java.util.Map.Entry");
//                            Types types = workingCopy.getTypes();
//                            TypeMirror tm = types.getArrayType(types.erasure(list.asType()));
//                            stats.add(treeMaker.Variable(treeMaker.Modifiers(Collections.<Modifier>emptySet()), "entry", treeMaker.Type(tm), null));
//
//                            ((JCMethodDecl) body).body.stats = stats;
//                        }
//
//                            messager.printMessage(Diagnostic.Kind.ERROR, method.getSimpleName());
//                    }));

//---------------------------------------------

            //get methods with @Elapsing
            final Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(annotation);
            for (final Element method : methods) {
                Elapsing elapsingAnnotation = method.getAnnotation(Elapsing.class);

                if (elapsingAnnotation != null) {
                   messager.printMessage(Diagnostic.Kind.ERROR, elapsingAnnotation.messageFormat());

                    if (elapsingAnnotation.maxElapsed() <= 0) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "@Elapsed: parameter maxElapsed must be greater than 0");
                    }

                    if (!elapsingAnnotation.messageFormat().contains("%s")) {
                        messager.printMessage(Diagnostic.Kind.WARNING, "@Elapsed: parameter format should contain '%s' otherwise elapsed time will not be logged");
                    }

                    JCTree methodDeclNode = utils.getTree(method);
                    if (methodDeclNode instanceof JCMethodDecl) {
                        // get method statements
                        final List<JCStatement> methodStatements = ((JCMethodDecl) methodDeclNode).body.stats;

                        // new method body
                        List<JCStatement> newStatements = List.nil();

//                        defineElapsingParams();
//                        //add start and finish time to the beginning of the method
//                        JCVariableDecl timeStartVarDecl = makeTimeVar(
//                                treeMaker, utils, "start", treeMaker.Modifiers(Flags.FINAL),
//                                makeCurrentTime(treeMaker, utils));
//                        newStatements = newStatements.append(timeStartVarDecl);
//
//                        JCVariableDecl timeFinishVarDecl = makeTimeVar(treeMaker, utils, "finish", treeMaker.Modifiers(0),
//                                treeMaker.Literal(0));
//                        newStatements = newStatements.append(timeFinishVarDecl);
//
//                        // create try block
//                        List<JCStatement> tryBlock = List.nil();
//
//                        //append try block with method statements
//                        for (JCStatement statement : methodStatements) {
//                            tryBlock = tryBlock.append(statement);
//                        }
//
//                        //save finish time
//                        tryBlock = tryBlock.append(treeMaker.Exec(treeMaker.Assign(
//                                treeMaker.Ident(utils.getName(timeFinishVarDecl.name)), makeCurrentTime(treeMaker, utils))));
//
//                        // create finally block, append with time logging
//                        JCBlock finallyBlock = makeLogBlock(treeMaker, utils, timeStartVarDecl, timeFinishVarDecl);
//
//                        //add try-finally block to newStatements
//                        newStatements = newStatements.append(treeMaker.Try(treeMaker.Block(0, tryBlock), List.nil(), finallyBlock));
//
//                        // save replace method body with new statements
//                        ((JCMethodDecl) methodDeclNode).body.stats = newStatements;
                    }
                }
            }
            return true;
        }
        return false;
    }


//    private void defineElapsingParams(Elapsing elapsingAnnotation){
////        if (elapsingAnnotation.messageFormat()==null){
////            elapsingAnnotation.messageFormat()
////        }
//
//    }
//
//    /**
//     * @param maker    treeMaker
//     * @param utils    javacElements
//     * @param elapsing @Elapsing annotation
//     * @return System.nanoTime() or System.currentTimeMillis() expression
//     */
//    private JCExpression makeCurrentTime(TreeMaker maker, JavacElements utils) {
//        // create System.nanoTime or System.currentTimeMillis call
//        JCExpression timeCallExpression = maker.Ident(utils.getName("System"));
//        String methodName;
//        switch (elapsing.timeUnit()) {
//            case NANOSECOND:
//                methodName = "nanoTime";
//                break;
//            default:
//                methodName = "currentTimeMillis";
//                break;
//        }
//        timeCallExpression = maker.Select(timeCallExpression, utils.getName(methodName));
//        return maker.Apply(List.nil(), timeCallExpression, List.nil());
//    }
//
//
//    /**
//     * creates time variable declaration and initialisation
//     *
//     * @param maker     treeMaket
//     * @param utils     javacElements
//     * @param name      time varible name
//     * @param modifiers variable modifiers
//     * @param value     variable initial value
//     * @return time variable declaration, eg final long time_start_1615004237="[value]";
//     */
//    private JCVariableDecl makeTimeVar(TreeMaker maker, JavacElements utils,
//                                       String name, JCModifiers modifiers, JCExpression value) {
//        //create new time variable definition
//        String fieldName = "time_" + name + "_" + System.currentTimeMillis();
//        return maker.VarDef(modifiers, utils.getName(fieldName), maker.TypeIdent(TypeTag.LONG), value);
//    }
//
//
//    /**
//     * creates log statement
//     *
//     * @param maker      treeMaker
//     * @param utils      javacElements
//     * @param level      logging level
//     * @param loggedText text that will be logged
//     * @return logging expression, eg Logger.getGlobal().log(level, loggedText));
//     */
//    private JCExpressionStatement logElapsed(TreeMaker maker, JavacElements utils,
//                                             String level, JCExpression loggedText) {
//        //Logger.getGlobal().log(Level.INFO, String.format(FORMAT, ELAPSED_TIME));
//        //Logger.getGlobal().log(level, loggedText);
//        //Logger.getGlobal().log
//        // JCExpression getLoggerExpression = maker.Ident(utils.getName(loggerClass.getSimpleName()));
////        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("util"));
////        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("logging"));
////        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("Logger"));
////        getLoggerExpression = maker.Select(getLoggerExpression, utils.getName("getGlobal"));
//       // ElapsingConfig.getInstance().getLogger()
//        JCExpression getLoggerInstance = maker.Ident(utils.getName("ElapsingConfig"));
//        getLoggerInstance=maker.Select(getLoggerInstance, utils.getName("getInstance"));
//
//        JCExpression getLoggerExpression = maker.Select(maker.Apply(List.nil(), getLoggerInstance, List.nil()),
//                utils.getName("getLogger"));
//        JCExpression logCallExpression = maker.Select(maker.Apply(List.nil(), getLoggerExpression, List.nil()),
//                utils.getName("log"));
//
//        //Level.INFO
//        JCExpression levelExpression = maker.Ident(utils.getName("java"));
//        levelExpression = maker.Select(levelExpression, utils.getName("util"));
//        levelExpression = maker.Select(levelExpression, utils.getName("logging"));
//        levelExpression = maker.Select(levelExpression, utils.getName("Level"));
//        levelExpression = maker.Select(levelExpression, utils.getName(level));
//
//        List<JCExpression> logArgs = List.nil();
//        logArgs = logArgs.append(levelExpression);
//        logArgs = logArgs.append(loggedText);
//
//
//        JCExpression logExpression = maker.Apply(List.nil(), logCallExpression, logArgs);
//        return maker.Exec(logExpression);
//    }
//
//    /**
//     * creates block of statements tht logs time
//     *
//     * @param maker         treeMaker
//     * @param utils         javacElements
//     * @param elapsing      @Elapsing
//     * @param timeStartVar  start time variable declaration
//     * @param timeFinishVar finish time variable declaration
//     * @return block of statements tht logs time
//     */
//    private JCBlock makeLogBlock(TreeMaker maker, JavacElements utils, , JCVariableDecl timeStartVar, JCVariableDecl timeFinishVar) {
//        //messager.printMessage(Diagnostic.Kind.ERROR, elapsing.logger().getSimpleName());
//        List<JCStatement> statements = List.nil();
//
//        //log elapsed time
//
//        // String.format(FORMAT, ELAPSED_TIME)
//        JCExpression elapsedTime = maker.Binary(Tag.MINUS, maker.Ident(utils.getName(timeFinishVar.name)), maker.Ident(timeStartVar.name));
//
//        JCExpression formatExpression = maker.Ident(utils.getName("String"));
//        formatExpression = maker.Select(formatExpression, utils.getName("format"));
//
//        List<JCExpression> formatArgs = List.nil();
//        formatArgs = formatArgs.append(maker.Literal(elapsing.messageFormat())); //format
//        formatArgs = formatArgs.append(elapsedTime); //ELAPSED_TIME
//
//        JCExpression format = maker.Apply(List.nil(), formatExpression, formatArgs);
//
//
//        JCExpressionStatement logElapsedTime = logElapsed(maker, utils, Level.INFO.getName().toUpperCase(), format);
//        statements = statements.append(logElapsedTime);
//        //----------------------------------------------------------------------------------------------------------------------------//
//        //log delta elapsed if method elapsed more than said
//        JCExpression condition = maker.Binary(
//                Tag.GT, //greater
//                maker.Binary(Tag.MINUS, makeCurrentTime(maker, utils, elapsing), maker.Ident(timeStartVar.name)),//currTime-startTime=elapsed
//                maker.Literal(elapsing.maxElapsed())); //maxElapsed
//
//        formatArgs = List.nil();
//        formatArgs = formatArgs.append(maker.Literal(elapsing.maxElapsedMessageFormat())); //format
//        formatArgs = formatArgs.append(maker.Binary(Tag.MINUS, elapsedTime, maker.Literal(elapsing.maxElapsed()))); //ELAPSED_TIME-maxElapsed
//        format = maker.Apply(List.nil(), formatExpression, formatArgs);
//        JCStatement ifStatement = maker.If(
//                condition,
//                //then
//                logElapsed(maker, utils, Level.WARNING.getName().toUpperCase(), format),
//                /*else*/ null);
//
//        statements = statements.append(ifStatement);
//        return maker.Block(0, statements);
//    }
}
