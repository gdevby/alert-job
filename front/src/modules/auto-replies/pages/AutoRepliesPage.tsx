import './autoRepliesPage.scss';

import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import { AccountsTab } from '@/modules/auto-replies/components/AccountsTab';
import { TemplatesTab } from '@/modules/auto-replies/components/TemplatesTab';
import { PromptsTab } from '@/modules/auto-replies/components/PromptsTab';
import { TestingTab } from '@/modules/auto-replies/components/TestingTab/TestingTab';

const tabs = {
  Accounts: 'accounts',
  Templates: 'templates',
  Prompts: 'prompts',
  Testing: 'testing'
} as const;

const AutoRepliesPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedTab, setSelectedTab] = useState<(typeof tabs)[keyof typeof tabs]>(
    Object.values(tabs).find(tab => tab === searchParams.get('tab')) ?? 'accounts',
  );

  const handleChange = (_: React.SyntheticEvent, tab: (typeof tabs)[keyof typeof tabs]) => {
    setSelectedTab(tab);
    setSearchParams({ tab });
  };

  return (
    <div className="auto-replies-page">
      <div className="container">
        <Tabs className="auto-replies-page__tabs" value={selectedTab} onChange={handleChange}>
          <Tab value={tabs.Accounts} label="Аккаунты" />
          <Tab value={tabs.Templates} label="Шаблоны" />
          <Tab value={tabs.Prompts} label="Промпты" />
          <Tab value={tabs.Testing} label="Тестирование автоответов" />
        </Tabs>

        {selectedTab === 'accounts' && <AccountsTab />}
        {selectedTab === 'templates' && <TemplatesTab />}
        {selectedTab === 'prompts' && <PromptsTab />}
        {selectedTab === 'testing' && <TestingTab />}
      </div>
    </div>
  );
};

export default AutoRepliesPage;
