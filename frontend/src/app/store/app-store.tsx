import * as React from 'react';

import { Actions, INITIAL_SESSION, Session, settingsContext } from 'app/context';

interface State {
    session: Session;
}

class AppStore extends React.Component<{}, State> {

    public readonly state: State = {
        session: INITIAL_SESSION,
    };

    public render(): React.ReactNode {
        const {
            children,
        } = this.props;

        const {
            session,
        } = this.state;

        const actions: Actions = { updateSession: this.updateSession };

        return (
            <settingsContext.Provider value={{ session, actions }}>
                {children}
            </settingsContext.Provider>
        );
    }

    private readonly updateSession = (session: Session): void => {
        this.setState({ session });
    };

}

export { AppStore };
