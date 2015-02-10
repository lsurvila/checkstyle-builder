package com.lsurvila.checkstyle;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FastStack;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class AndroidDeclarationOrderCheck extends Check {

    /** State for the VARIABLE_DEF */
    private static final int STATE_STATIC_VARIABLE_DEF = 1;

    /** State for the VARIABLE_DEF */
    private static final int STATE_INSTANCE_VARIABLE_DEF = 2;

    /** State for the CTOR_DEF */
    private static final int STATE_CTOR_DEF = 3;

    /** State for the METHOD_DEF */
    private static final int STATE_METHOD_DEF = 4;

    /**
     * List of Declaration States. This is necessary due to
     * inner classes that have their own state
     */
    private final FastStack<ScopeState> scopeStates = FastStack.newInstance();

    /**
     * private class to encapsulate the state
     */
    private static class ScopeState
    {
        /** The state the check is in */
        private int scopeState = STATE_STATIC_VARIABLE_DEF;

        /** The sub-state the check is in */
        private Scope declarationAccess = Scope.PUBLIC;
    }

    /** If true, ignores the check to constructors. */
    private boolean ignoreConstructors;
    /** If true, ignore the check to methods. */
    private boolean ignoreMethods;
    /** If true, ignore the check to modifiers (fields, ...). */
    private boolean ignoreModifiers;
    /** If true, ignore the check to parcelable creator modifier */
    private boolean ignoreParcelableCreator;

    @Override
    public int[] getDefaultTokens()
    {
        return new int[] {
                TokenTypes.CTOR_DEF,
                TokenTypes.METHOD_DEF,
                TokenTypes.MODIFIERS,
                TokenTypes.OBJBLOCK,
        };
    }

    @Override
    public void visitToken(DetailAST ast)
    {
        final int parentType = ast.getParent().getType();
        ScopeState state;

        switch (ast.getType()) {
            case TokenTypes.OBJBLOCK:
                scopeStates.push(new ScopeState());
                break;

            case TokenTypes.CTOR_DEF:
                if (parentType != TokenTypes.OBJBLOCK) {
                    return;
                }

                state = scopeStates.peek();
                if (state.scopeState > STATE_CTOR_DEF) {
                    if (!ignoreConstructors) {
                        log(ast, "Constructor definition in wrong order.");
                    }
                }
                else {
                    state.scopeState = STATE_CTOR_DEF;
                }
                break;

            case TokenTypes.METHOD_DEF:
                state = scopeStates.peek();
                if (parentType != TokenTypes.OBJBLOCK) {
                    return;
                }

                if (state.scopeState > STATE_METHOD_DEF) {
                    if (!ignoreMethods) {
                        log(ast, "Method definition in wrong order.");
                    }
                }
                else {
                    state.scopeState = STATE_METHOD_DEF;
                }
                break;

            case TokenTypes.MODIFIERS:
                if ((parentType != TokenTypes.VARIABLE_DEF)
                        || (ast.getParent().getParent().getType()
                        != TokenTypes.OBJBLOCK))
                {
                    return;
                }

                state = scopeStates.peek();
                // if it's static
                if (ast.findFirstToken(TokenTypes.LITERAL_STATIC) != null) {
                    // if previous was not static
                    if (state.scopeState > STATE_STATIC_VARIABLE_DEF) {
                        if (!ignoreModifiers
                                // if previous was a method or constructor
                                || state.scopeState > STATE_INSTANCE_VARIABLE_DEF)
                        {
                            // if it's not parcelable creator
                            if (!ignoreParcelableCreator || !isParcelableCreator(ast))
                            {
                                log(ast, "Static variable definition in wrong order.");
                            }
                        }
                    }
                    else {
                        state.scopeState = STATE_STATIC_VARIABLE_DEF;
                    }
                }
                else {
                    if (state.scopeState > STATE_INSTANCE_VARIABLE_DEF) {
                        log(ast, "Instance variable definition in wrong order.");
                    }
                    else if (state.scopeState == STATE_STATIC_VARIABLE_DEF) {
                        state.declarationAccess = Scope.PUBLIC;
                        state.scopeState = STATE_INSTANCE_VARIABLE_DEF;
                    }
                }

                final Scope access = ScopeUtils.getScopeFromMods(ast);
                if (state.declarationAccess.compareTo(access) > 0) {
                    if (!ignoreModifiers) {
                        log(ast, "Variable access definition in wrong order.");
                    }
                }
                else {
                    state.declarationAccess = access;
                }
                break;

            default:
        }
    }

    private boolean isParcelableCreator(DetailAST ast) {
        boolean isPublic = ast.findFirstToken(TokenTypes.LITERAL_PUBLIC) != null;
        boolean isStatic = ast.findFirstToken(TokenTypes.LITERAL_STATIC) != null;
        boolean isParcelableType = false;
        boolean isCreatorName = false;
        if (ast.getNextSibling().getType() == TokenTypes.TYPE) {
            DetailAST typeAst = ast.getNextSibling();
            if (typeAst.getFirstChild().getType() == TokenTypes.DOT) {
                DetailAST dotAst = typeAst.getFirstChild();
                isParcelableType = dotAst.findFirstToken(TokenTypes.IDENT) != null && dotAst.findFirstToken(TokenTypes.IDENT).getText().equals("Parcelable");
            }
        }
        if (ast.getNextSibling().getNextSibling().getType() == TokenTypes.IDENT) {
            DetailAST identAst = ast.getNextSibling().getNextSibling();
            isCreatorName = identAst.getText().equals("CREATOR");
        }
        return isPublic && isStatic && isParcelableType && isCreatorName;
    }

    @Override
    public void leaveToken(DetailAST ast)
    {
        switch (ast.getType()) {
            case TokenTypes.OBJBLOCK:
                scopeStates.pop();
                break;

            default:
        }
    }

    /**
     * Sets whether to ignore constructors.
     * @param ignoreConstructors whether to ignore constructors.
     */
    public void setIgnoreConstructors(boolean ignoreConstructors)
    {
        this.ignoreConstructors = ignoreConstructors;
    }

    /**
     * Sets whether to ignore methods.
     * @param ignoreMethods whether to ignore methods.
     */
    public void setIgnoreMethods(boolean ignoreMethods)
    {
        this.ignoreMethods = ignoreMethods;
    }

    /**
     * Sets whether to ignore modifiers.
     * @param ignoreModifiers whether to ignore modifiers.
     */
    public void setIgnoreModifiers(boolean ignoreModifiers)
    {
        this.ignoreModifiers = ignoreModifiers;
    }

    public void setIgnoreParcelableCreator(boolean ignoreParcelableCreator) {
        this.ignoreParcelableCreator = ignoreParcelableCreator;
    }

}
